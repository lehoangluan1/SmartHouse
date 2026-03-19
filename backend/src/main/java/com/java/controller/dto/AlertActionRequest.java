package com.java.controller.dto;

import jakarta.validation.constraints.NotNull;

public record AlertActionRequest(
        @NotNull(message = "userId cannot be null") Long userId
) {}
