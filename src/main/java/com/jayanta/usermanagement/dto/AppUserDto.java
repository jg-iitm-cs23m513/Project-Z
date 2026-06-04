package com.jayanta.usermanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jayanta.usermanagement.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppUserDto {
    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private String licenseActivatedBy;

    //  DATE ONLY - yyyy-MM-dd format
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime licenseActivatedOn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime licenseExpiredOn;

    private Boolean isInternal;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime lastModifiedAt;

    private String modifiedBy;
    private Boolean licenseValid;

    public static AppUserDto from(com.jayanta.usermanagement.model.AppUser user) {
        return AppUserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .licenseActivatedBy(user.getLicenseActivatedBy())
                .licenseActivatedOn(user.getLicenseActivatedOn())
                .licenseExpiredOn(user.getLicenseExpiredOn())
                .isInternal(user.getIsInternal())
                .createdAt(user.getCreatedAt())
                .lastModifiedAt(user.getLastModifiedAt())
                .modifiedBy(user.getModifiedBy())
                .licenseValid(user.isLicenseValid())
                .build();
    }
}
