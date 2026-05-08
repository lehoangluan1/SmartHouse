package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.NotFoundException;
import com.java.domain.DeviceClass;
import com.java.persistence.entity.ControlCommandEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeModeControlService {

    private static final String MODE_CAPABILITY = "MODE";

    private final DeviceRepository deviceRepository;
    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final DeviceControlService deviceControlService;

    @Transactional
    public ControlCommandEntity changeMode(
            Long homeId,
            String mode,
            String source,
            Long sourceRefId,
            UserEntity actor,
            String actorName
    ) {
        DeviceEntity controller = deviceRepository.findFirstByHomeIdAndDeviceClass(
                homeId,
                DeviceClass.CONTROLLER
        ).orElseThrow(() -> new NotFoundException("Controller device does not exist for home: " + homeId));

        deviceRuntimeStateService.syncModeForHome(
                homeId,
                mode,
                sourceLabel(source),
                sourceRefId,
                actor
        );

        return deviceControlService.controlDevice(
                controller,
                MODE_CAPABILITY,
                mode,
                source,
                actor,
                actorName
        ).command();
    }

    private String sourceLabel(String source) {
        if (source == null) {
            return "SYSTEM_CONTROL";
        }
        return switch (source.trim().toLowerCase()) {
            case "manual" -> "MANUAL_CONTROL";
            case "automation" -> "AUTO_CONTROL";
            case "scheduler" -> "MODE_SCHEDULE";
            case "scene" -> "SCENE_CONTROL";
            default -> "SYSTEM_CONTROL";
        };
    }
}
