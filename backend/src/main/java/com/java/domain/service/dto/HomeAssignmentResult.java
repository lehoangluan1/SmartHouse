package com.java.domain.service.dto;

import com.java.domain.HomeUserRole;
import com.java.persistence.entity.HomeEntity;

public record HomeAssignmentResult(
        HomeEntity home,
        HomeUserRole roleInHome,
        boolean createdNewHome
) {
}