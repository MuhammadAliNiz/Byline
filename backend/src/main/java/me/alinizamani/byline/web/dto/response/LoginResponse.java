package me.alinizamani.byline.web.dto.response;

import me.alinizamani.byline.domain.user.UserRole;

import java.util.UUID;

public record LoginResponse (
        String accessToken,
        String tokenType,
        int expiresIn,
        User user
){
    public record User(
            UUID userId,
            String username,
            String email,
            String firstName,
            String lastName,
            String avatarUrl,
            UserRole role,
            boolean emailVerified
    ){}

}
