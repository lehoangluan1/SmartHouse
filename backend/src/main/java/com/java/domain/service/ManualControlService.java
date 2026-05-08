package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.controller.dto.ControlCommandResponse;
import com.java.controller.dto.ControlRequest;
import com.java.domain.SystemMode;
import com.java.mapper.ControlCommandMapper;
import com.java.persistence.entity.DeviceEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManualControlService {

    private final ManualControlRequestNormalizer requestNormalizer;
    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final ModeManualControlHandler modeManualControlHandler;
    private final DeviceCommandExecutionService deviceCommandExecutionService;
    private final ControlCommandMapper controlCommandMapper;
    private final HomeModeResolver homeModeResolver;
    private final ManualHoldService manualHoldService;
    private final HomeModeControlService homeModeControlService;

    @Transactional
    public ControlCommandResponse execute(DeviceEntity device, ControlRequest request) {
        Long homeId = device.getHome().getId();

        NormalizedControlRequest normalized = requestNormalizer.normalize(device, request);

        if (!deviceRuntimeStateService.hasChanged(
                device.getId(),
                normalized.target(),
                normalized.value()
        )) {
            return controlCommandMapper.toStatusResponse(
                    device.getId(),
                    normalized.target(),
                    normalized.value(),
                    request.actorId(),
                    request.actorName(),
                    "NO_OP"
            );
        }

        if ("MODE".equals(normalized.target())) {
            return modeManualControlHandler.handle(device, request, normalized);
        }

        SystemMode currentMode = homeModeResolver.resolveHomeMode(homeId, SystemMode.auto);

        if (currentMode != SystemMode.manual) {
            homeModeControlService.changeMode(
                    homeId,
                    SystemMode.manual.name(),
                    "manual",
                    null,
                    null,
                    request.actorName()
            );
            manualHoldService.enableManualHold(
                    device,
                    currentMode,
                    request.actorId(),
                    request.actorName()
            );
        }

        return deviceCommandExecutionService.executeManual(device, request, normalized);
    }
}
