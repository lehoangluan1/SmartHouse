package com.java.controller.dto;

import com.java.domain.AuthProvider;
import com.java.domain.HomeUserRole;
import com.java.domain.SystemUserRole;
import com.java.domain.UserStatus;

public record UserProvisionResponse(
        Long userId,
        String username,
        AuthProvider provider,
        SystemUserRole systemRole,
        HomeUserRole homeRole,
        UserStatus status,
        boolean mustChangePassword,
        boolean notificationQueued,
        Long homeId,
        String homeName,
        String homeAddress
) {
}