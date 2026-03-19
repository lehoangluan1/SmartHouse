package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.controller.dto.ControlCommandResponse;
import com.java.controller.dto.ControlRequest;
import com.java.domain.SystemMode;
import com.java.mapper.ControlCommandMapper;
import com.java.persistence.entity.DeviceEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModeManualControlHandler {

    private final DeviceTargetPolicy deviceTargetPolicy;
    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final ActivityLogService activityLogService;
    private final ActivityLogPayloadBuilder activityLogPayloadBuilder;
    private final ModeAutomationService modeAutomationService;
    private final ControlCommandMapper controlCommandMapper;
    private final ManualHoldService manualHoldService;

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

        String modeValue = String.valueOf(normalized.value());
        SystemMode nextMode = deviceTargetPolicy.parseSystemMode(modeValue);

        manualHoldService.clearHold(
                device.getId(),
                request.actorId(),
                request.actorName(),
                "EXPLICIT_MODE_CHANGE"
        );

        deviceRuntimeStateService.syncModeForHome(
                device.getHome().getId(),
                nextMode.name(),
                "MODE_SYNC",
                null,
                null
        );

        activityLogService.log(
                device.getHome().getId(),
                device.getId(),
                request.actorId(),
                "MANUAL_CONTROL",
                request.method(),
                null,
                null,
                activityLogPayloadBuilder.controlPayload("mode", modeValue)
        );

        modeAutomationService.evaluateAllByHome(device.getHome().getId());

        return controlCommandMapper.toStatusResponse(
                device.getId(),
                "mode",
                modeValue,
                request.actorId(),
                request.actorName(),
                "SYNCED"
        );
    }
}