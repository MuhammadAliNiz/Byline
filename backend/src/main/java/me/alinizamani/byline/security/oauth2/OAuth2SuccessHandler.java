package me.alinizamani.byline.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.alinizamani.byline.domain.user.RefreshToken;
import me.alinizamani.byline.domain.user.User;
import me.alinizamani.byline.repository.RefreshTokenRepository;
import me.alinizamani.byline.service.JwtService;
import me.alinizamani.byline.util.CookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieUtils cookieUtils;
    @Value("${app.jwt.refresh-token-expiration-s}")
    private int refreshExpirationInSeconds;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            Authentication authentication
    )throws IOException {
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String jti = UUID.randomUUID().toString();
        String refreshTokenJwt = jwtService.generateRefreshToken(user, jti);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setJti(jti);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshExpirationInSeconds));
        refreshToken.setIpAddress(httpServletRequest.getRemoteAddr());
        refreshToken.setUserAgent(httpServletRequest.getHeader("User-Agent"));
        refreshTokenRepository.save(refreshToken);

        ResponseCookie cookie = cookieUtils.createHttpOnlyCookie(
                "refreshToken",
                refreshTokenJwt,
                refreshExpirationInSeconds,
                "/api/auth"
        );

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        clearAuthenticationAttributes(httpServletRequest);
        HttpSession session = httpServletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        log.info("OAuth2 login success for user: {}", user.getEmail());

        String redirectUrl = frontendBaseUrl + "/oauth2/callback";

        getRedirectStrategy().sendRedirect(httpServletRequest, httpServletResponse, redirectUrl);
    }
}
