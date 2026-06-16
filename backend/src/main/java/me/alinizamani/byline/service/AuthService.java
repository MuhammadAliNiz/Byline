package me.alinizamani.byline.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import me.alinizamani.byline.domain.user.User;
import me.alinizamani.byline.web.dto.internal.LoginResult;
import me.alinizamani.byline.web.dto.request.ChangePasswordRequest;
import me.alinizamani.byline.web.dto.request.LoginRequest;
import me.alinizamani.byline.web.dto.request.RegisterRequest;
import me.alinizamani.byline.web.dto.request.ResetPasswordRequest;
import me.alinizamani.byline.web.dto.response.LoginResponse;

public interface AuthService {
    void register(RegisterRequest registerRequest);

    LoginResult login(@Valid LoginRequest loginRequest, String ipAddress, String userAgent);

    LoginResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    void verifyEmail(String token);

    void forgotPassword(@Valid @Email String email);

    void resetPassword(@Valid ResetPasswordRequest resetPasswordRequest);

    void changePassword(User user, ChangePasswordRequest changePasswordRequest);
}
