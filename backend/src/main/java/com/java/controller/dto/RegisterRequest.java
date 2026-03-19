package com.java.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username cannot be empty")
        @Size(min = 3, max = 64, message = "username must be between 3 and 64 characters")
        String username,

        @NotBlank(message = "password cannot be empty")
        @Size(min = 6, max = 100, message = "password must be at least 6 characters")
        String password
) {
}