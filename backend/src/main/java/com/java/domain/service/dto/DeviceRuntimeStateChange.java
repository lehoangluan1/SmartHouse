package com.java.domain.service.dto;

import com.java.persistence.entity.DeviceRuntimeStateEntity;

public record DeviceRuntimeStateChange(
        DeviceRuntimeStateEntity entity,
        Object previousValue,
        Object nextValue,
        boolean changed,
        boolean historyRecorded
) {
}