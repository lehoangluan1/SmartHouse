package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.domain.DeviceClass;
import com.java.domain.SystemMode;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.repo.DeviceRepository;
import com.java.persistence.repo.DeviceRuntimeStateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HomeModeResolver {

    private final DeviceRepository deviceRepository;
    private final DeviceRuntimeStateRepository deviceRuntimeStateRepository;

    public SystemMode resolveHomeMode(Long homeId, SystemMode fallbackMode) {
        SystemMode defaultMode = fallbackMode != null ? fallbackMode : SystemMode.auto;

        return deviceRepository.findFirstByHomeIdAndDeviceClass(homeId, DeviceClass.CONTROLLER)
                .map(controller -> deviceRuntimeStateRepository.findByIdDeviceId(controller.getId()).stream()
                        .filter(s -> "MODE".equalsIgnoreCase(s.getCapabilityCode()))
                        .findFirst()
                        .map(DeviceRuntimeStateEntity::getValueText)
                        .map(this::parseModeSafe)
                        .orElse(defaultMode))
                .orElse(defaultMode);
    }

    private SystemMode parseModeSafe(String value) {
        if (value == null || value.isBlank()) {
            return SystemMode.auto;
        }

        try {
            return SystemMode.valueOf(value.trim().toLowerCase());
        } catch (Exception e) {
            return SystemMode.auto;
        }
    }
}