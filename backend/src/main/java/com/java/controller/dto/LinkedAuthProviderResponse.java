package com.java.controller.dto;

import java.time.OffsetDateTime;

public record LinkedAuthProviderResponse(
        String provider,
        String providerEmail,
        OffsetDateTime linkedAt
) {}