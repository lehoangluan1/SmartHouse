package com.java.domain.service.dto;

import java.time.OffsetDateTime;

public record RefreshTokenPairResponse(
        String refreshToken,
        OffsetDateTime refreshTokenExpiresAt
) {
}
