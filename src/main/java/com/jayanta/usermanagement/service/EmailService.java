package com.jayanta.usermanagement.service;

import com.jayanta.usermanagement.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:default-noreply@yourapp.com}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Async
    public void sendOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔐 Your OTP Code - User Management Service");
            message.setText(String.format("""
                🚀 Your OTP Code: **%s**
                
                ⏰ Valid for 10 minutes only
                🔒 If you didn't request this, ignore safely
                📱 App: %s
                
                -- User Management Service
                """, otp, frontendUrl));

            mailSender.send(message);
            log.info(" OTP email sent successfully to: {}", toEmail);

        } catch (MailAuthenticationException e) {
            log.error("Email authentication failed for sender: {}. Check Gmail App Password!", fromEmail);
            throw new UserException(
                    "Email service not configured. Please contact administrator.",
                    "EMAIL_CONFIG_ERROR"
            );

        } catch (MailException e) {
            log.error("Failed to send OTP to {}: {}", toEmail, e.getMessage());
            throw new UserException(
                    String.format("Failed to send OTP to %s. Please try again.", toEmail),
                    "EMAIL_SEND_FAILED"
            );

        } catch (Exception e) {
            log.error(" Unexpected error sending OTP to {}: {}", toEmail, e.getMessage());
            throw new UserException(
                    "Email service temporarily unavailable. Please try again later.",
                    "EMAIL_SERVICE_ERROR"
            );
        }
    }
}
