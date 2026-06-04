package com.jayanta.usermanagement.dto;

import com.jayanta.usermanagement.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @NotBlank private String name;
    @NotNull private String role;
    private Boolean isInternal;
    private String licenseActivatedBy;
    private String licenseActivatedOn;
    private String licenseExpiredOn;
    private String modifiedBy;
}