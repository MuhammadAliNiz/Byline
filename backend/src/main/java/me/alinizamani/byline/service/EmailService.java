package me.alinizamani.byline.service;

import jakarta.validation.constraints.Email;

public interface EmailService {
    void sendVerificationEmail(String to, String firstName, String token);

    void sendPasswordResetEmail(@Email String email, String rawToken);
}
