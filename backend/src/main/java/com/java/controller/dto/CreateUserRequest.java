package com.java.controller.dto;

import com.java.domain.AuthProvider;
import com.java.domain.HomeUserRole;
import com.java.domain.SystemUserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank(message = "Username must not be blank")
        String username,

        @NotNull(message = "Provider must not be null")
        AuthProvider provider,

        @NotNull(message = "System role must not be null")
        SystemUserRole systemRole,

        @NotNull(message = "Home assignment mode must not be null")
        HomeAssignmentMode homeAssignmentMode,

        Long homeId,

        String homeName,

        String address,

        @NotNull(message = "Home role must not be null")
        HomeUserRole homeRole
) {
}