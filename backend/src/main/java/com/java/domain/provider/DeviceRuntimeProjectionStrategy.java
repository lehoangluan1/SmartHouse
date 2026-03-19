package com.java.domain.provider;

import com.java.domain.service.dto.DeviceStateProjection;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import java.util.Map;

public interface DeviceRuntimeProjectionStrategy {
    boolean supports(String subtype);
    void apply(Map<String, DeviceRuntimeStateEntity> stateMap, DeviceStateProjection projection);
}