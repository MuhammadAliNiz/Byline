package me.alinizamani.byline.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.alinizamani.byline.domain.user.*;
import me.alinizamani.byline.exception.ConflictException;
import me.alinizamani.byline.exception.EmailNotVerifiedException;
import me.alinizamani.byline.exception.UnauthorizedException;
import me.alinizamani.byline.repository.*;
import me.alinizamani.byline.service.AuthService;
import me.alinizamani.byline.service.EmailService;
import me.alinizamani.byline.service.JwtService;
import me.alinizamani.byline.web.dto.internal.LoginResult;
import me.alinizamani.byline.web.dto.request.ChangePasswordRequest;
import me.alinizamani.byline.web.dto.request.LoginRequest;
import me.alinizamani.byline.web.dto.request.RegisterRequest;
import me.alinizamani.byline.web.dto.request.ResetPasswordRequest;
import me.alinizamani.byline.web.dto.response.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.jwt-expiration-s}")
    private int jwtExpirationInSeconds;

    @Value("${app.jwt.refresh-token-expiration-s}")
    private long refreshExpirationInSeconds;


    @Transactional
    @Override
    public void register(RegisterRequest registerRequest) {
        String email    = registerRequest.email().trim().toLowerCase();
        String username = registerRequest.username().trim();

        userRepository.findByEmail(email).ifPresent(
                existingUser -> {
                    if (!existingUser.isEmailVerified()) {
                        userRepository.delete(existingUser);
                        userRepository.flush();
                    } else {
                        if (existingUser.getAuthProvider() == AuthProvider.GOOGLE) {
                            throw new ConflictException("An account with this email is already registered via Google. Please log in with Google.");
                        }
                        throw new ConflictException("An account with this email already exists.");
                    }
                }
        );

        if (userProfileRepository.existsByUsername(username)) {
            throw new ConflictException("This username is already taken.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);

        userRepository.save(user);

        UserProfile userProfile = new UserProfile();
        userProfile.setUser(user);
        userProfile.setFirstName(registerRequest.firstName());
        userProfile.setLastName(registerRequest.lastName());
        userProfile.setUsername(username);
        userProfile.setUsernameUpdatedAt(Instant.now());

        userProfileRepository.save(userProfile);

        String rawToken = UUID.randomUUID().toString();

        EmailVerificationToken emailVerificationToken = new EmailVerificationToken();
        emailVerificationToken.setUser(user);
        emailVerificationToken.setTokenHash(hashToken(rawToken));
        emailVerificationToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        emailVerificationTokenRepository.save(emailVerificationToken);

        emailService.sendVerificationEmail(user.getEmail(), userProfile.getFirstName(), rawToken);
    }

    @Transactional
    @Override
    public LoginResult login(LoginRequest loginRequest, String ipAddress, String userAgent) {
        String email = loginRequest.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Bad credentials"));

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new UnauthorizedException("You registered using Google. Please log in with your Google account.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, loginRequest.password())
        );

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId()).orElseThrow();

        String accessToken = jwtService.generateAccessToken(user);

        LoginResponse loginResponse = new LoginResponse(
                accessToken,
                "Bearer",
                jwtExpirationInSeconds,
                new LoginResponse.User(
                        user.getId(),
                        userProfile.getUsername(),
                        user.getEmail(),
                        userProfile.getFirstName(),
                        userProfile.getLastName(),
                        userProfile.getAvatarUrl(),
                        user.getRole(),
                        user.isEmailVerified()
                )
        );

        String jti = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setJti(jti);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshExpirationInSeconds));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);

        refreshTokenRepository.save(refreshToken);

        String refreshTokenJwt = jwtService.generateRefreshToken(user, jti);

        return new LoginResult(refreshTokenJwt, loginResponse);
    }

    @Transactional
    @Override
    public LoginResponse refreshToken(String refreshToken) {
            String jti = jwtService.extractAndValidateRefreshToken(refreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token. Please log in again."));

        if(storedToken.isRevoked()) {
            throw new UnauthorizedException("This refresh token has been revoked. Please log in again.");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw  new UnauthorizedException("Expired refresh token. Please log in again.");
        }

        if(!jwtService.extractUserIdFromToken(refreshToken).equals(storedToken.getUser().getId())) {
            throw new UnauthorizedException("Refresh token does not match the user. Please log in again.");
        }

        String newAccessToken = jwtService.generateAccessToken(storedToken.getUser());


        return new LoginResponse(
                newAccessToken,
                "Bearer",
                jwtExpirationInSeconds,
                null
        );
    }

    @Transactional
    @Override
    public void logout(String refreshToken) {
        try{
        String jti = jwtService.extractAndValidateRefreshToken(refreshToken);

        refreshTokenRepository.findByJti(jti).ifPresent(storedToken -> {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
        });
        } catch (Exception e) {
            log.warn("Failed to revoke refresh token: {}", e.getMessage());
        }
    }

    @Transactional
    @Override
    public void verifyEmail(String token) {
        String hashedToken = hashToken(token);

        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired email verification token."));

        if (verificationToken.isUsed()) {
            throw new ConflictException("This token has already been used. Your email is already verified.");
        }

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Verification token has expired. Please request a new one.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email successfully verified for user: {}", user.getEmail());
    }

    @Transactional
    @Override
    public void forgotPassword(String email) {
        Optional<User> user = userRepository.findByEmail(email.trim().toLowerCase());

        if(user.isPresent()) {
            if (user.get().getAuthProvider() != AuthProvider.LOCAL) {
                return;
            }

            if (!user.get().isEmailVerified()) {
                return;
            }

            passwordResetTokenRepository.deleteAllByUser(user.get());
            passwordResetTokenRepository.flush();

            String rawToken = UUID.randomUUID().toString();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user.get());
            resetToken.setTokenHash(hashToken(rawToken));
            resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            passwordResetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.get().getEmail(), rawToken);

            log.info("Reset email sent to {}", user.get().getEmail());
        }
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashToken(resetPasswordRequest.token()))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired password reset token."));

        if(resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Password reset token has expired. Please request a new one.");
        }

        if (resetToken.isUsed()) {
            throw new ConflictException("This token has already been used. Please request a new password reset.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(resetPasswordRequest.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    @Transactional
    @Override
    public void changePassword(User user, ChangePasswordRequest changePasswordRequest) {
        if(user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new UnauthorizedException("Password change is not allowed for accounts registered via " + user.getAuthProvider());
        }

        if (!passwordEncoder.matches(changePasswordRequest.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userRepository.save(user);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
