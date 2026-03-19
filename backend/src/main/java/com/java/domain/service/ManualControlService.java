package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.NotFoundException;
import com.java.config.BadRequestException;
import com.java.controller.dto.ControlCommandResponse;
import com.java.controller.dto.ControlRequest;
import com.java.domain.SystemMode;
import com.java.mapper.ControlCommandMapper;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManualControlService {

    private final DeviceRepository deviceRepository;
    private final ManualControlRequestNormalizer requestNormalizer;
    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final ModeManualControlHandler modeManualControlHandler;
    private final DeviceCommandExecutionService deviceCommandExecutionService;
    private final ControlCommandMapper controlCommandMapper;
    private final HomeModeResolver homeModeResolver;
    private final ManualHoldService manualHoldService;
    private final HomeAccessGuard homeAccessGuard; 

    @Transactional
    public ControlCommandResponse execute(Long deviceId, ControlRequest request) {
        DeviceEntity device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        if (device.getHome() == null || device.getHome().getId() == null) {
            throw new BadRequestException("Device is not associated with any home");
        }

        Long homeId = device.getHome().getId();
        homeAccessGuard.requireActivatedProfile(homeId);

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

        if (device.getHome() != null && device.getHome().getId() != null) {
            SystemMode currentMode = homeModeResolver.resolveHomeMode(homeId, SystemMode.auto);

            if (currentMode != SystemMode.manual) {
                deviceRuntimeStateService.syncModeForHome(homeId, SystemMode.manual.name());
                manualHoldService.enableManualHold(
                        device,
                        currentMode,
                        request.actorId(),
                        request.actorName()
                );
            }
        }

        return deviceCommandExecutionService.executeManual(device, request, normalized);
    }
}