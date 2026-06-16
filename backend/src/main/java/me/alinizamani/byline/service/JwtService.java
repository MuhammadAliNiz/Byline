package me.alinizamani.byline.service;

import me.alinizamani.byline.domain.user.User;

import java.util.UUID;

public interface JwtService {
    String generateAccessToken(User user);

    String generateRefreshToken(User user, String jti);

    UUID extractAndValidateAccessToken(String token);

    String extractAndValidateRefreshToken(String token);

    UUID extractUserIdFromToken(String token);
}
