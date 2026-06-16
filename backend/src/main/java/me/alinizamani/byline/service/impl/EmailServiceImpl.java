package me.alinizamani.byline.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.alinizamani.byline.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendVerificationEmail(String to, String firstName, String token) {
        try {
            String verificationUrl = frontendBaseUrl + "/verify-email?token=" + token;

            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("verificationUrl", verificationUrl);

            String html = templateEngine.process("emails/verify-email", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Email Verification");
            helper.setFrom(fromEmail);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            log.info("Email verification email has been sent to {}", to);

        } catch (Exception e) {
            log.error("Failed to send email verification email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String rawToken) {
        try {
            String resetUrl = frontendBaseUrl + "/reset-password?token=" + rawToken;

            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);

            String html = templateEngine.process("emails/reset-password", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Password Reset Request");
            helper.setFrom(fromEmail);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            log.info("Password reset email has been sent to {}", to);

        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }
}