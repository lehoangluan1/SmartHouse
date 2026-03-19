package com.java.controller.dto;

import java.time.OffsetDateTime;

public record UserAuthProviderItemResponse(
        String provider,
        String providerEmail,
        OffsetDateTime linkedAt
) {}