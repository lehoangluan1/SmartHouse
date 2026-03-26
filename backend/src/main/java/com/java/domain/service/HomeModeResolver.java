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

    private static final String MODE_CAPABILITY = "MODE";

    private final DeviceRepository deviceRepository;
    private final DeviceRuntimeStateRepository deviceRuntimeStateRepository;

    public SystemMode resolveHomeMode(Long homeId, SystemMode fallbackMode) {
        SystemMode controllerMode = deviceRepository.findFirstByHomeIdAndDeviceClass(homeId, DeviceClass.CONTROLLER)
                .map(controller -> deviceRuntimeStateRepository.findByIdDeviceId(controller.getId()).stream()
                        .filter(s -> MODE_CAPABILITY.equalsIgnoreCase(s.getCapabilityCode()))
                        .findFirst()
                        .map(DeviceRuntimeStateEntity::getValueText)
                        .map(value -> parseModeSafe(value, null))
                        .orElse(null))
                .orElse(null);

        if (controllerMode != null) {
            return controllerMode;
        }

        SystemMode anyModeInHome = deviceRepository.findByHomeId(homeId).stream()
                .map(device -> deviceRuntimeStateRepository.findByIdDeviceId(device.getId()).stream()
                        .filter(s -> MODE_CAPABILITY.equalsIgnoreCase(s.getCapabilityCode()))
                        .findFirst()
                        .map(DeviceRuntimeStateEntity::getValueText)
                        .map(value -> parseModeSafe(value, null))
                        .orElse(null))
                .filter(mode -> mode != null)
                .findFirst()
                .orElse(null);

        return anyModeInHome != null ? anyModeInHome : fallbackMode;
    }

    private SystemMode parseModeSafe(String value, SystemMode fallbackMode) {
        if (value == null || value.isBlank()) {
            return fallbackMode;
        }

        try {
            return SystemMode.valueOf(value.trim().toLowerCase());
        } catch (Exception e) {
            return fallbackMode;
        }
    }
}