package com.java.controller.dto;

public record ActivateHomeProfileResponse(
        Long userId,
        Long homeId,
        String roleInHome,
        boolean allowProfileActivation,
        boolean isPrimary
) {}