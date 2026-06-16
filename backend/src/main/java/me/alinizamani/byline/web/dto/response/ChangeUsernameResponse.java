package me.alinizamani.byline.web.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ChangeUsernameResponse(
        UUID userId,
        String oldUsername,
        String newUsername,
        Instant updatedAt
) {
}
