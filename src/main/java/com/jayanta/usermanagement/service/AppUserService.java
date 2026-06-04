package com.jayanta.usermanagement.service;

import com.jayanta.usermanagement.dto.RegisterRequest;
import com.jayanta.usermanagement.dto.UpdateUserRequest;
import com.jayanta.usermanagement.enums.UserRole;
import com.jayanta.usermanagement.exception.UserException;
import com.jayanta.usermanagement.model.AppUser;
import com.jayanta.usermanagement.model.Role;
import com.jayanta.usermanagement.repository.RoleRepository;
import com.jayanta.usermanagement.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUser register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new UserException("Email already exists", "EMAIL_EXISTS", "email");
        }
        Role userRole = roleRepository.findByName(UserRole.User)
                .orElseThrow(() -> new UserException("User role not found", "ROLE_NOT_FOUND"));
        AppUser user = new AppUser();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);
        user.setCreatedAt(LocalDateTime.now());
        AppUser savedUser = appUserRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());
        return savedUser;
    }

    public AppUser findByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND"));
    }

    public List<AppUser> getAllUsers() {
        return appUserRepository.findAll();
    }

    public List<AppUser> getInternalUsers() {
        return appUserRepository.findAllInternalUsers();
    }

    public AppUser updateUser(Long userId, UpdateUserRequest request) {
        try {
            AppUser user = appUserRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found: " + userId, "USER_NOT_FOUND"));

            Role userRole = roleRepository.findByName(UserRole.valueOf(request.getRole()))
                    .orElseThrow(() -> new UserException("Role not found: " + request.getRole(), "ROLE_NOT_FOUND"));
            user.setRole(userRole);

            String securityContextUser = SecurityContextHolder.getContext().getAuthentication().getName();
            user.setModifiedBy(request.getModifiedBy() != null ? request.getModifiedBy() : securityContextUser);
            user.setLastModifiedAt(LocalDateTime.now());

            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                user.setName(request.getName().trim());
            }

            if (request.getIsInternal() != null) {
                user.setIsInternal(request.getIsInternal());
            }

            // License fields only for external Users
            if (!user.getIsInternal() && "User".equals(request.getRole())) {
                if (request.getLicenseActivatedBy() != null && !request.getLicenseActivatedBy().trim().isEmpty()) {
                    user.setLicenseActivatedBy(request.getLicenseActivatedBy().trim());
                }


                if (request.getLicenseActivatedOn() != null && !request.getLicenseActivatedOn().trim().isEmpty()) {
                    LocalDate date = LocalDate.parse(request.getLicenseActivatedOn(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    user.setLicenseActivatedOn(date.atStartOfDay());
                }
                if (request.getLicenseExpiredOn() != null && !request.getLicenseExpiredOn().trim().isEmpty()) {
                    LocalDate date = LocalDate.parse(request.getLicenseExpiredOn(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    user.setLicenseExpiredOn(date.atStartOfDay());
                }

                // Date validation
                if (user.getLicenseActivatedOn() != null && user.getLicenseExpiredOn() != null) {
                    if (user.getLicenseExpiredOn().isBefore(user.getLicenseActivatedOn())) {
                        throw new UserException("License expiry date cannot be before activation date", "INVALID_LICENSE_DATES");
                    }
                }
            }

            AppUser savedUser = appUserRepository.save(user);
            log.info("User fully updated by {}: {}", user.getModifiedBy(), savedUser.getId());
            return savedUser;
        } catch (UserException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new UserException("Invalid role: " + request.getRole(), "INVALID_ROLE");
        } catch (Exception e) {
            log.error("Failed to update user {}: {}", userId, e.getMessage());
            throw new UserException("Failed to update user: " + e.getMessage(), "UPDATE_FAILED");
        }
    }

    public void deleteUser(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with UserId : "+userId, "USER_NOT_FOUND"));
        appUserRepository.delete(user);
        log.info("User deleted: {}", userId);
    }

    public void deleteUsers(List<Long> userIds) {
        log.info("Admin bulk deleting {} users", userIds.size());
        userIds.forEach(this::deleteUser);
        log.info("Bulk delete completed");
    }

    public void resetPassword(String email, String newPassword) {
        log.info("Resetting password for: {}", email);
        AppUser user = findByEmail(email);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastModifiedAt(LocalDateTime.now());
        appUserRepository.save(user);
        log.info("Password reset successful for user: {}", user.getId());
    }
}
