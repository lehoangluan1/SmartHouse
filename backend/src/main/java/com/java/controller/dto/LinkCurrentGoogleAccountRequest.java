package com.java.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LinkCurrentGoogleAccountRequest(
        @NotBlank String authorizationCode,
        @NotBlank String redirectUri
) {}