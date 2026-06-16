package me.alinizamani.byline.web.dto.request;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProfileRequest(
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @Size(max = 300, message = "Bio cannot exceed 300 characters")
        String bio,
        @URL(message = "Must be a valid URL")
        String websiteUrl,

        @Size(max = 50, message = "Twitter handle cannot exceed 50 characters")
        String twitterHandle,

        @URL(message = "Must be a valid URL")
        String linkedinUrl,

        @URL(message = "Must be a valid URL")
        String githubUrl
) {
}
