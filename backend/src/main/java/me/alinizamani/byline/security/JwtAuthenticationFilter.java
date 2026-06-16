package me.alinizamani.byline.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.alinizamani.byline.domain.user.User;
import me.alinizamani.byline.service.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try{
            final UUID userId = jwtService.extractAndValidateAccessToken(jwt);
            final User user = customUserDetailsService.loadUserById(userId);

            if (!user.isEnabled() || !user.isAccountNonLocked()) {
                log.info("Rejected valid JWT for inactive account [{}] on [{}]",
                        userId, request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT on [{}]: {}", request.getRequestURI(), e.getMessage());

        } catch (SignatureException e) {
            log.warn("JWT signature invalid on [{}]: {}", request.getRequestURI(), e.getMessage());

        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT on [{}]: {}", request.getRequestURI(), e.getMessage());

        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT on [{}]: {}", request.getRequestURI(), e.getMessage());

        } catch (IllegalArgumentException e) {
            log.warn("Blank/null JWT on [{}]: {}", request.getRequestURI(), e.getMessage());

        } catch (UsernameNotFoundException e) {
            log.info("JWT references deleted user on [{}]: {}", request.getRequestURI(), e.getMessage());

        }
        filterChain.doFilter(request, response);
    }
}
