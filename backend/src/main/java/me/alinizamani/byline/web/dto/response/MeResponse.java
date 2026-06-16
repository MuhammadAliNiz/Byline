package me.alinizamani.byline.web.dto.response;

import me.alinizamani.byline.domain.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record MeResponse (
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String bio,
        String avatarUrl,
        String websiteUrl,
        String twitterHandle,
        String linkedinUrl,
        String githubUrl,
        UserRole role,
        boolean emailVerified,
        long followersCount,
        long followingCount,
        long articlesCount,
        Instant createdAt,
        Instant updatedAt
){
}
