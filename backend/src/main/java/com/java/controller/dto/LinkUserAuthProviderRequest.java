package com.java.controller.dto;

import com.java.domain.AuthProvider;

import jakarta.validation.constraints.NotNull;

public record LinkUserAuthProviderRequest(
        @NotNull AuthProvider provider,
        String providerEmail
) {}