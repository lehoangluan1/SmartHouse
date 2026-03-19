package com.java.domain.provider;

import com.java.domain.service.CapabilityValueSupport;
import com.java.domain.service.dto.DeviceCapabilities;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceRuntimeMetadataResolver {

    private final CapabilityValueSupport capabilityValueSupport;

    public String resolveMode(Map<String, DeviceRuntimeStateEntity> stateMap) {
        DeviceRuntimeStateEntity entity = stateMap.get(DeviceCapabilities.MODE);
        return entity == null ? null : capabilityValueSupport.runtimeValueAsString(entity);
    }

    public OffsetDateTime resolveLatestUpdatedAt(Map<String, DeviceRuntimeStateEntity> stateMap) {
        return stateMap.values().stream()
                .map(DeviceRuntimeStateEntity::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
    }

    public String resolveDeviceType(DeviceEntity device) {
        if (device.getSubtype() != null && !device.getSubtype().isBlank()) {
            return device.getSubtype();
        }
        return device.getDeviceClass() != null ? device.getDeviceClass().name() : null;
    }
}