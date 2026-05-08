package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.config.NotFoundException;
import com.java.domain.events.ControlCommandEvent;
import com.java.eventing.DomainEventBus;
import com.java.persistence.entity.ControlCommandEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.ControlCommandRepository;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceControlService {

    private final DeviceRepository deviceRepository;
    private final DeviceTargetPolicy deviceTargetPolicy;
    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final ControlCommandFactory controlCommandFactory;
    private final ControlCommandRepository controlCommandRepository;
    private final ControlCommandSender controlCommandSender;
    private final CapabilityValueSupport capabilityValueSupport;
    private final DomainEventBus eventBus;

    @Transactional
    public ControlResult controlDevice(
            Long homeId,
            String deviceKey,
            String target,
            String value,
            String source
    ) {
        DeviceEntity device = deviceRepository.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new NotFoundException("Device does not exist: " + deviceKey));

        if (homeId != null && (device.getHome() == null || !homeId.equals(device.getHome().getId()))) {
            throw new BadRequestException("Device does not belong to home: " + homeId);
        }

        String normalizedTarget = deviceTargetPolicy.normalizeTarget(target);
        Object normalizedValue = deviceTargetPolicy.normalizeValue(normalizedTarget, value);

        return controlDevice(
                device,
                normalizedTarget,
                normalizedValue,
                normalizeSource(source),
                null,
                normalizeSource(source)
        );
    }

    @Transactional
    public ControlResult controlDevice(
            DeviceEntity device,
            String normalizedTarget,
            Object normalizedValue,
            String source,
            UserEntity actor,
            String actorName
    ) {
        if (device == null || device.getId() == null) {
            throw new BadRequestException("Invalid device");
        }

        String normalizedSource = normalizeSource(source);
        deviceTargetPolicy.validateTargetForDevice(device, normalizedTarget);

        ControlCommandEntity command = actor == null
                ? controlCommandFactory.createWithSource(
                        device,
                        normalizedTarget,
                        normalizedValue,
                        actorName,
                        normalizedSource
                )
                : controlCommandFactory.createManual(
                        device,
                        normalizedTarget,
                        normalizedValue,
                        actor,
                        actorName
                );
        command.setSource(normalizedSource);

        command = controlCommandRepository.save(command);

        log.info(
                "COMMAND created source={} homeId={} deviceKey={} target={} value={} id={}",
                normalizedSource,
                device.getHome() != null ? device.getHome().getId() : null,
                device.getDeviceKey(),
                externalTarget(normalizedTarget),
                externalValue(command),
                command.getId()
        );

        eventBus.publish(new ControlCommandEvent(
                command.getId(),
                device.getHome() != null ? device.getHome().getId() : null,
                device.getId(),
                device.getDeviceKey(),
                normalizedTarget,
                capabilityValueSupport.commandValueAsString(command),
                normalizedSource
        ));

        DeviceRuntimeStateService.StateWriteResult stateWriteResult =
                deviceRuntimeStateService.upsertValueAndRecordHistory(
                        device.getId(),
                        normalizedTarget,
                        normalizedValue,
                        sourceLabel(normalizedSource),
                        command.getId(),
                        actor
                );

        command = dispatchBestEffort(command);
        return new ControlResult(command, stateWriteResult);
    }

    private ControlCommandEntity dispatchBestEffort(ControlCommandEntity command) {
        try {
            return controlCommandSender.sendNow(command);
        } catch (RuntimeException ex) {
            log.warn("COMMAND dispatch fail id={} err={}", command.getId(), ex.getClass().getSimpleName());
            command.setStatus(com.java.domain.CommandStatus.PENDING);
            return controlCommandRepository.save(command);
        }
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "system";
        }
        return source.trim().toLowerCase();
    }

    private String sourceLabel(String source) {
        return switch (source) {
            case "manual" -> "MANUAL_CONTROL";
            case "automation" -> "AUTO_CONTROL";
            case "scheduler" -> "MODE_SCHEDULE";
            case "scene" -> "SCENE_CONTROL";
            default -> "SYSTEM_CONTROL";
        };
    }

    private String externalTarget(String target) {
        if (target == null) {
            return null;
        }
        return switch (target.trim().toUpperCase()) {
            case "POWER" -> "power";
            case "SPEED" -> "fan_speed";
            case "BRIGHTNESS" -> "brightness";
            case "MODE" -> "mode";
            default -> target.trim().toLowerCase();
        };
    }

    private String externalValue(ControlCommandEntity command) {
        if (command.getValueBoolean() != null) {
            return command.getValueBoolean() ? "on" : "off";
        }
        String value = capabilityValueSupport.commandValueAsString(command);
        return value == null ? null : value.toLowerCase();
    }

    public record ControlResult(
            ControlCommandEntity command,
            DeviceRuntimeStateService.StateWriteResult stateWriteResult
    ) {
    }
}
