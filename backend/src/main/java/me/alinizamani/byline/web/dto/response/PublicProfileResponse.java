package me.alinizamani.byline.web.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PublicProfileResponse(
        UUID userId,
        String username,
        String firstName,
        String lastName,
        String bio,
        String avatarUrl,
        String websiteUrl,
        String twitterHandle,
        String linkedinUrl,
        String githubUrl,
        long followersCount,
        long followingCount,
        long articlesCount,
        boolean isFollowing,
        Instant createdAt
) {
}
