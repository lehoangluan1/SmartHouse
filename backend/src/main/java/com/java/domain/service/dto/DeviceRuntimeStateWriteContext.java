package com.java.domain.service.dto;

import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.entity.UserEntity;

public record DeviceRuntimeStateWriteContext(
        DeviceEntity device,
        DeviceRuntimeStateEntity currentState,
        String capabilityCode,
        Object requestedValue,
        String source,
        Long sourceRefId,
        UserEntity changedBy
) {
}