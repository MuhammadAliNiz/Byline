package me.alinizamani.byline.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import static java.util.Arrays.stream;

@Component
@RequiredArgsConstructor
public class CookieUtils {
    public ResponseCookie createHttpOnlyCookie(String name, String value, long maxAgeInSeconds, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path(path)
                .maxAge(maxAgeInSeconds)
                .build();
    }

    public String getCookieValue(HttpServletRequest httpServletRequest, String refreshToken) {
        if (httpServletRequest.getCookies() != null) {
            return stream(httpServletRequest.getCookies())
                    .filter(cookie -> cookie.getName().equals(refreshToken))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }

    public ResponseCookie deleteHttpOnlyCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path(path)
                .maxAge(0)
                .sameSite("None")
                .build();
    }
}
