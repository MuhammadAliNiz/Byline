package me.alinizamani.byline.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String path = request.getRequestURI();
        String timestamp = Instant.now().toString();

        // Construct the JSON payload manually to avoid needing ObjectMapper
        String jsonPayload = String.format(
                "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Authentication required. Please provide a valid token.\", \"path\": \"%s\", \"timestamp\": \"%s\"}",
                path, timestamp
        );

        // Write the JSON directly to the response
        try (PrintWriter writer = response.getWriter()) {
            writer.print(jsonPayload);
            writer.flush();
        }
    }
}