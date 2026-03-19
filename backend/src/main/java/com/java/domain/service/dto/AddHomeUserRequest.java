package com.java.domain.service.dto;

import com.java.domain.HomeUserRole;

import jakarta.validation.constraints.NotNull;

public record AddHomeUserRequest(
        @NotNull Long userId,
        @NotNull HomeUserRole roleInHome,
        Boolean allowProfileActivation,
        Boolean isPrimary
) {}