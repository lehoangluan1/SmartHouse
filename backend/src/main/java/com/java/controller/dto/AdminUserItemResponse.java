package com.java.controller.dto;

import java.time.OffsetDateTime;

import com.java.domain.AuthProvider;
import com.java.domain.SystemUserRole;
import com.java.domain.UserStatus;

public record AdminUserItemResponse(
        Long id,
        String username,
        AuthProvider provider,
        SystemUserRole role,
        UserStatus status,
        boolean mustChangePassword,
        Long homeId,
        OffsetDateTime invitedAt
) {}