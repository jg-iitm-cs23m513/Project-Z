package com.jayanta.usermanagement.controller;

import com.jayanta.usermanagement.dto.AppUserDto;
import com.jayanta.usermanagement.dto.UpdateUserRequest;
import com.jayanta.usermanagement.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all users (Any authenticated user)")
    public ResponseEntity<List<AppUserDto>> getUsers() {
        return ResponseEntity.ok(
                appUserService.getAllUsers().stream().map(AppUserDto::from).collect(Collectors.toList())
        );
    }

    @GetMapping("/internal")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get internal users (Any authenticated user)")
    public ResponseEntity<List<AppUserDto>> getInternalUsers() {
        return ResponseEntity.ok(
                appUserService.getInternalUsers().stream().map(AppUserDto::from).collect(Collectors.toList())
        );
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Update user (Admin only) - ALL fields except ID")
    public ResponseEntity<AppUserDto> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        AppUserDto updatedUser = AppUserDto.from(appUserService.updateUser(userId, request));
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Delete single user (Admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        appUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Delete multiple users (Admin only)")
    public ResponseEntity<Void> deleteUsers(@RequestBody List<Long> userIds) {
        appUserService.deleteUsers(userIds);
        return ResponseEntity.noContent().build();
    }
}
