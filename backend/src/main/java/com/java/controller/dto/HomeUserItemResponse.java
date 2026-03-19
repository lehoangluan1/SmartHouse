package com.java.controller.dto;

public record HomeUserItemResponse(
        Long userId,
        String username,
        String systemRole,
        String roleInHome,
        String status,
        String provider,
        boolean allowProfileActivation,
        boolean mustChangePassword,
        boolean isPrimary
) {}