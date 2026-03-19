package com.java.controller.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String username,
        String role,
        String roleInHome,
        String status,
        boolean mustChangePassword,
        Long homeId
) {}