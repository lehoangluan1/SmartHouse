package com.java.controller.dto;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        Long homeId
) {}