package com.java.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.controller.dto.ControlCommandResponse;
import com.java.controller.dto.ControlRequest;
import com.java.domain.SystemMode;
import com.java.mapper.ControlCommandMapper;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.DeviceRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModeManualControlHandler {

    private final DeviceTargetPolicy deviceTargetPolicy;
    private final ActivityLogService activityLogService;
    private final ActivityLogPayloadBuilder activityLogPayloadBuilder;
    private final ControlCommandMapper controlCommandMapper;
    private final ManualHoldService manualHoldService;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final AutoControlService autoControlService;
    private final HomeModeControlService homeModeControlService;

    public ControlCommandResponse handle(
            DeviceEntity device,
            ControlRequest request,
            NormalizedControlRequest normalized
    ) {
        if (!deviceTargetPolicy.supportsModeSync(device)) {
            throw new BadRequestException(
                    "Device class " + device.getDeviceClass() + " does not support target mode"
            );
        }

        if (device.getHome() == null || device.getHome().getId() == null) {
            throw new BadRequestException("Device has not yet been assigned to a home so mode cannot be synced");
        }

        Long homeId = device.getHome().getId();
        String modeValue = String.valueOf(normalized.value());
        SystemMode nextMode = deviceTargetPolicy.parseSystemMode(modeValue);

        manualHoldService.clearHold(
                device.getId(),
                request.actorId(),
                request.actorName(),
                "EXPLICIT_MODE_CHANGE"
        );

        var actor = Optional.ofNullable(request.actorId())
                .map(userRepository::getReferenceById)
                .orElse(null);

        var command = homeModeControlService.changeMode(
                homeId,
                nextMode.name(),
                "manual",
                null,
                actor,
                request.actorName()
        );

        if (nextMode == SystemMode.sleep || nextMode == SystemMode.away) {
            deviceRepository.findByHomeId(homeId).stream()
                    .filter(this::isLight)
                    .forEach(light -> autoControlService.execute(
                            light,
                            "POWER",
                            "false",
                            "MODE_FORCE_LIGHT_OFF"
                    ));
        }

        activityLogService.log(
                homeId,
                device.getId(),
                request.actorId(),
                "MANUAL_CONTROL",
                request.method(),
                null,
                null,
                activityLogPayloadBuilder.controlPayload("mode", modeValue)
        );

        return controlCommandMapper.toResponse(command);
    }

    private boolean isLight(DeviceEntity device) {
        return device != null
                && device.getDeviceClass() != null
                && "ACTUATOR".equalsIgnoreCase(device.getDeviceClass().name())
                && device.getSubtype() != null
                && "LIGHT".equalsIgnoreCase(device.getSubtype().trim())
                && deviceTargetPolicy.supportsPower(device);
    }
}
