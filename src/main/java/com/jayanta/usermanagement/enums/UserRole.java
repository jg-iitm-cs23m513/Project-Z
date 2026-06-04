package com.jayanta.usermanagement.enums;


public enum UserRole {
    Admin,
    ProjectDirector,
    SecurityHead,
    ReleaseEngineer,
    User;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
