package me.alinizamani.byline.web.dto.internal;

import me.alinizamani.byline.web.dto.response.LoginResponse;

public record LoginResult(
        String refreshToken,
        LoginResponse loginResponse
) {
}
