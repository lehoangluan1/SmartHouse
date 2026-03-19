package com.java.domain.service.dto;

import com.java.domain.HomeUserRole;

public record UpdateHomeUserRequest(
        HomeUserRole roleInHome,
        Boolean allowProfileActivation,
        Boolean isPrimary
) {}