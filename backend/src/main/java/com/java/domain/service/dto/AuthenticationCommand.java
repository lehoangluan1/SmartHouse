package com.java.domain.service.dto;

import com.java.domain.AuthProvider;

public record AuthenticationCommand(
        AuthProvider provider,
        String username,
        String password,
        String authorizationCode,
        String redirectUri
) {
}