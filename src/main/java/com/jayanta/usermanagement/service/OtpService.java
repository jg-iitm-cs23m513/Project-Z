package com.jayanta.usermanagement.service;

import com.jayanta.usermanagement.exception.UserException;
import com.jayanta.usermanagement.model.Otp;
import com.jayanta.usermanagement.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    public String generateAndSendOtp(String email) {
        log.info("Generating OTP for email: {}", email);

        // : Delete existing OTPs (ignore if none exist)
        try {
            otpRepository.deleteByEmail(email);
            log.debug("Cleared existing OTPs for: {}", email);
        } catch (Exception e) {
            log.debug("No existing OTPs found for {}: {}", email, e.getMessage());
            // Continue - no OTPs existed, that's fine!
        }

        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        Otp otpEntity = new Otp();
        otpEntity.setEmail(email);
        otpEntity.setCode(otp);
        otpEntity.setExpiresAt(expiresAt);

        otpRepository.save(otpEntity);
        emailService.sendOtp(email, otp);

        log.info("OTP sent successfully to: {}", email);
        return otp;
    }

    public boolean verifyOtp(String email, String code) {
        log.debug("Verifying OTP for email: {}, code: {}", email, code);

        Optional<Otp> otpOpt = otpRepository.findByEmailAndCode(email, code);

        if (otpOpt.isEmpty()) {
            log.warn("No OTP found for email: {} or wrong code", email);
            throw new UserException("Invalid OTP code", "INVALID_OTP");
        }

        Otp otp = otpOpt.get();
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for email: {}", email);
            otpRepository.deleteByEmail(email);
            throw new UserException("OTP expired", "OTP_EXPIRED");
        }

        // Delete used OTP
        otpRepository.deleteByEmail(email);
        log.info("OTP verified successfully for: {}", email);
        return true;
    }

    private String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}
