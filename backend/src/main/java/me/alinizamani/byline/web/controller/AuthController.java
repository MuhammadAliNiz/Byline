package me.alinizamani.byline.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.alinizamani.byline.domain.user.User;
import me.alinizamani.byline.exception.UnauthorizedException;
import me.alinizamani.byline.service.AuthService;
import me.alinizamani.byline.util.CookieUtils;
import me.alinizamani.byline.web.dto.internal.LoginResult;
import me.alinizamani.byline.web.dto.request.*;
import me.alinizamani.byline.web.dto.response.ApiResponse;
import me.alinizamani.byline.web.dto.response.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    private final CookieUtils cookieUtils;

    @Value("${app.jwt.refresh-token-expiration-s}")
    private int refreshExpirationInSeconds;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest registerRequest) {

        authService.register(registerRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Check your email to verify your account"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Email verified successfully. You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login (
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        String ipAddress = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        LoginResult loginResult = authService.login(loginRequest, ipAddress, userAgent);


        ResponseCookie cookie = cookieUtils.createHttpOnlyCookie(
                "refreshToken",
                loginResult.refreshToken(),
                refreshExpirationInSeconds,
                "/api/auth"
        );

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());


        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(loginResult.loginResponse(), "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(HttpServletRequest httpServletRequest) {
            String refreshToken = cookieUtils.getCookieValue(httpServletRequest, "refreshToken");
            if (refreshToken == null) {
                throw new UnauthorizedException("Refresh token is missing. Please log in again.");
            }

            LoginResponse loginResponse = authService.refreshToken(refreshToken);

            return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        String refreshToken = cookieUtils.getCookieValue(httpServletRequest, "refreshToken");

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }


        ResponseCookie deleteCookie = cookieUtils.deleteHttpOnlyCookie(
                "refreshToken",
                "/api/auth"
        );

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return  ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Logout successful"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {

        authService.resetPassword(resetPasswordRequest);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Password reset successful. You can now log in with your new password."));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<?>> changePassword(
            @RequestBody ChangePasswordRequest changePasswordRequest,
            @AuthenticationPrincipal User user
    ) {
        authService.changePassword(user, changePasswordRequest);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Password changed successfully."));
    }
}
