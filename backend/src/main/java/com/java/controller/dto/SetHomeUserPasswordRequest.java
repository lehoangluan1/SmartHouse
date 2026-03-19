package com.java.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetHomeUserPasswordRequest(
        @NotBlank(message = "New password cannot be empty")
        @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
        String newPassword,

        Boolean requireChangeOnNextLogin
) {
}
