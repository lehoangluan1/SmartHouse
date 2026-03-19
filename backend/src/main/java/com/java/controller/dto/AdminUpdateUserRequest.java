package com.java.controller.dto;

import com.java.domain.SystemUserRole;
import com.java.domain.UserStatus;

import jakarta.validation.constraints.NotNull;

public record AdminUpdateUserRequest(
        @NotNull(message = "Role cannot be empty")
        SystemUserRole role,

        @NotNull(message = "Status cannot be empty")
        UserStatus status
) {}