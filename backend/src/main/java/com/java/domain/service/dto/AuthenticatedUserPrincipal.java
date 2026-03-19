package com.java.domain.service.dto;

import org.springframework.security.core.AuthenticatedPrincipal;

import com.java.domain.HomeUserRole;
import com.java.domain.SystemUserRole;

public record AuthenticatedUserPrincipal(
        Long id,
        String username,
        SystemUserRole role,
        HomeUserRole roleInHome,
        boolean mustChangePassword,
        String status
) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return username;
    }
}