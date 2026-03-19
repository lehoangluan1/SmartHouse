package com.java.controller.dto;

import com.java.domain.SystemUserRole;

public record MyProfileResponse(
        Long userId,
        String username,
        SystemUserRole role,
        String status,
        boolean mustChangePassword,
        Long homeId
) {}