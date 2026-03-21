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
import com.java.eventing.DeviceStateChangedEvent;
import com.java.eventing.DomainEventBus;
import com.java.eventing.HomeModeChangedEvent;
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
    private final DomainEventBus eventBus;

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

        publishDashboardEvent(device, request.request().target(), request.request().value());

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

        if (executed) {
            publishDashboardEvent(device, request.target(), request.value());
        }

        return executed
                ? ControlExecutionResult.autoExecuted()
                : ControlExecutionResult.autoNoOp();
    }

    private void publishDashboardEvent(DeviceEntity device, String target, String value) {
        if (device == null || device.getHome() == null || device.getHome().getId() == null) {
            return;
        }

        Long homeId = device.getHome().getId();
        Long deviceId = device.getId();
        String normalizedTarget = target == null ? "" : target.trim().toLowerCase();
        String normalizedValue = value == null ? "" : value.trim();

        switch (normalizedTarget) {
            case "fan" -> eventBus.publish(DeviceStateChangedEvent.builder()
                    .homeId(homeId)
                    .deviceId(deviceId)
                    .status(normalizeOnOff(normalizedValue))
                    .build());

            case "fanspeed" -> eventBus.publish(DeviceStateChangedEvent.builder()
                    .homeId(homeId)
                    .deviceId(deviceId)
                    .status(parsePercent(normalizedValue) > 0 ? "ON" : "OFF")
                    .speed(parsePercent(normalizedValue))
                    .build());

            case "light" -> eventBus.publish(DeviceStateChangedEvent.builder()
                    .homeId(homeId)
                    .deviceId(deviceId)
                    .status(normalizeOnOff(normalizedValue))
                    .build());

            case "brightness" -> eventBus.publish(DeviceStateChangedEvent.builder()
                    .homeId(homeId)
                    .deviceId(deviceId)
                    .status(parsePercent(normalizedValue) > 0 ? "ON" : "OFF")
                    .brightness(parsePercent(normalizedValue))
                    .build());

            case "mode" -> eventBus.publish(HomeModeChangedEvent.builder()
                    .homeId(homeId)
                    .deviceId(deviceId)
                    .mode(normalizedValue.toUpperCase())
                    .build());

            default -> {
            }
        }
    }

    private String normalizeOnOff(String value) {
        return "ON".equalsIgnoreCase(value) ? "ON" : "OFF";
    }

    private Integer parsePercent(String value) {
        try {
            int num = Integer.parseInt(value);
            if (num < 0) return 0;
            if (num > 100) return 100;
            return num;
        } catch (Exception ex) {
            return 0;
        }
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