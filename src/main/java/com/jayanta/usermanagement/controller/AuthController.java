package com.jayanta.usermanagement.controller;

import com.jayanta.usermanagement.dto.*;
import com.jayanta.usermanagement.model.AppUser;
import com.jayanta.usermanagement.security.JwtService;
import com.jayanta.usermanagement.service.OtpService;
import com.jayanta.usermanagement.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Registration - Authentication & Authorization")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserService userService;
    private final OtpService otpService;

    @Operation(summary = "Register new user")
    @PostMapping("/register")
    public ResponseEntity<AppUser> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        AppUser user = userService.register(request);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "User login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        AppUser user = (AppUser) authentication.getPrincipal();
        String jwtToken = jwtService.generateToken(user);
        LoginResponse response = LoginResponse.builder()
                .token(jwtToken)
                .user(AppUserDto.from(user))
                .build();
        log.info("User logged in successfully: {}", user.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Forgot password - send OTP")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        otpService.generateAndSendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent to your email!"));
    }

    @Operation(summary = "Verify OTP")
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Boolean>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("OTP verification for email: {}", request.getEmail());
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of("otpValid", isValid));
    }

    @Operation(summary = "Reset password")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request for email: {}", request.getEmail());
        userService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully!"));
    }
}
