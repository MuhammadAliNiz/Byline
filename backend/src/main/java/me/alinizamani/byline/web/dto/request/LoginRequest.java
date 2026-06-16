package me.alinizamani.byline.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email must not be blank")
        @Email
        String email,

        @NotBlank(message = "Password must not be blank")
        String password
) {
}
