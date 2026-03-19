package com.java.controller.dto;

import com.java.domain.AuthProvider;

import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull(message = "provider cannot be empty")
        AuthProvider provider,

        String username,
        String password,

        String authorizationCode,
        String redirectUri
) {
}