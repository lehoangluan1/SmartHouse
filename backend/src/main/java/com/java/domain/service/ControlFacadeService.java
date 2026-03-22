package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.config.NotFoundException;
import com.java.controller.dto.AutoControlExecutionRequest;
import com.java.controller.dto.ControlCommandResponse;
import com.java.controller.dto.ControlExecutionRequest;
import com.java.controller.dto.ControlExecutionResult;
import com.java.controller.dto.ManualControlExecutionRequest;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ControlFacadeService {

    private final DeviceRepository deviceRepository;
    private final HomeAccessGuard homeAccessGuard;
    private final ManualControlService manualControlService;
    private final AutoControlService autoControlService;

    @Transactional
    public ControlExecutionResult control(ControlExecutionRequest request) {
        if (request == null) {
            throw new BadRequestException("Control request must not be null");
        }

        DeviceEntity device = loadAndValidateDevice(request.deviceId());
        ensureHomeActivated(device);

        if (request instanceof ManualControlExecutionRequest manualRequest) {
            return handleManual(device, manualRequest);
        }

        if (request instanceof AutoControlExecutionRequest autoRequest) {
            return handleAuto(device, autoRequest);
        }

        throw new BadRequestException("Unsupported control request type");
    }

    private ControlExecutionResult handleManual(
            DeviceEntity device,
            ManualControlExecutionRequest request
    ) {
        if (request.request() == null) {
            throw new BadRequestException("Manual control payload must not be null");
        }

        ControlCommandResponse response =
                manualControlService.execute(device, request.request());

        return ControlExecutionResult.manual(response);
    }

    private ControlExecutionResult handleAuto(
            DeviceEntity device,
            AutoControlExecutionRequest request
    ) {
        if (request.target() == null || request.target().isBlank()) {
            throw new BadRequestException("Auto control target must not be blank");
        }

        if (request.value() == null || request.value().isBlank()) {
            throw new BadRequestException("Auto control value must not be blank");
        }

        boolean executed = autoControlService.execute(
                device,
                request.target(),
                request.value(),
                request.method()
        );

        return executed
                ? ControlExecutionResult.autoExecuted()
                : ControlExecutionResult.autoNoOp();
    }

    private DeviceEntity loadAndValidateDevice(Long deviceId) {
        if (deviceId == null) {
            throw new BadRequestException("Device id must not be null");
        }

        DeviceEntity device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        if (device.getHome() == null || device.getHome().getId() == null) {
            throw new BadRequestException("Device is not associated with any home");
        }

        return device;
    }

    private void ensureHomeActivated(DeviceEntity device) {
        homeAccessGuard.requireActivatedProfile(device.getHome().getId());
    }
}