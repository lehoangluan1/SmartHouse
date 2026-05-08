package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.config.NotFoundException;
import com.java.controller.dto.NextCommandResponse;
import com.java.domain.CommandStatus;
import com.java.domain.events.ControlCommandEvent;
import com.java.eventing.DomainEventBus;
import com.java.mapper.ControlCommandMapper;
import com.java.persistence.entity.ControlCommandEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.ControlCommandRepository;
import com.java.persistence.repo.DeviceRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ControlCommandService {

    private final DeviceRepository deviceRepo;
    private final ControlCommandRepository cmdRepo;
    private final DomainEventBus bus;
    private final DeviceTargetPolicy deviceTargetPolicy;
    private final ControlCommandMapper controlCommandMapper;
    private final ControlCommandFactory controlCommandFactory;
    private final CapabilityValueSupport capabilityValueSupport;
    private final CommandLongPollNotifier commandLongPollNotifier;

    public ControlCommandService(
            DeviceRepository deviceRepo,
            ControlCommandRepository cmdRepo,
            DomainEventBus bus,
            DeviceTargetPolicy deviceTargetPolicy,
            ControlCommandMapper controlCommandMapper,
            ControlCommandFactory controlCommandFactory,
            CapabilityValueSupport capabilityValueSupport,
            CommandLongPollNotifier commandLongPollNotifier
    ) {
        this.deviceRepo = deviceRepo;
        this.cmdRepo = cmdRepo;
        this.bus = bus;
        this.deviceTargetPolicy = deviceTargetPolicy;
        this.controlCommandMapper = controlCommandMapper;
        this.controlCommandFactory = controlCommandFactory;
        this.capabilityValueSupport = capabilityValueSupport;
        this.commandLongPollNotifier = commandLongPollNotifier;
    }

    @Transactional
    public void requestCommand(Long deviceId, String target, String value, String actor) {
        String normalizedTarget = deviceTargetPolicy.normalizeTarget(target);
        Object normalizedValue = deviceTargetPolicy.normalizeValue(normalizedTarget, value);

        DeviceEntity device = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device does not exist: " + deviceId));

        deviceTargetPolicy.validateTargetForDevice(device, normalizedTarget);

        if (existsPendingSame(device.getId(), normalizedTarget, normalizedValue)) {
            return;
        }

        String source = normalizeSource(actor);
        ControlCommandEntity command = controlCommandFactory.createWithSource(
                device,
                normalizedTarget,
                normalizedValue,
                actor,
                source
        );

        if (command.getStatus() == null) {
            command.setStatus(CommandStatus.PENDING);
        }

        command = cmdRepo.save(command);

        log.info(
                "COMMAND created source={} homeId={} deviceKey={} target={} value={} id={}",
                source,
                device.getHome() != null ? device.getHome().getId() : null,
                device.getDeviceKey(),
                normalizedTarget,
                capabilityValueSupport.commandValueAsString(command),
                command.getId()
        );

        bus.publish(new ControlCommandEvent(
                command.getId(),
                device.getHome() != null ? device.getHome().getId() : null,
                device.getId(),
                device.getDeviceKey(),
                normalizedTarget,
                normalizedValue,
                source
        ));
    }

    @Transactional
    public void requestCommandByDeviceKey(String deviceKey, String target, String value, String actor) {
        DeviceEntity device = deviceRepo.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new NotFoundException("Device does not exist: " + deviceKey));

        requestCommand(device.getId(), target, value, actor);
    }

    @Transactional
    public NextCommandResponse getNextCommand(String deviceKey) {
        return getNextCommand(deviceKey, 0L);
    }

    @Transactional
    public NextCommandResponse getNextCommand(String deviceKey, Long waitMs) {
        DeviceEntity device = deviceRepo.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new NotFoundException("Device does not exist: " + deviceKey));

        ControlCommandEntity cmd = findNextDeliverable(device);
        if (cmd == null && waitMs != null && waitMs > 0) {
            commandLongPollNotifier.await(deviceKey, Math.min(waitMs, 5000L));
            cmd = findNextDeliverable(device);
        }

        return cmd == null ? null : controlCommandMapper.toNextResponse(cmd);
    }

    private ControlCommandEntity findNextDeliverable(DeviceEntity device) {
        ControlCommandEntity cmd = cmdRepo.findNextDeliverable(device.getId());
        if (cmd == null) {
            return null;
        }

        if (cmd.getStatus() == CommandStatus.PENDING) {
            cmd.setStatus(CommandStatus.SENT);
            if (cmd.getSentAt() == null) {
                cmd.setSentAt(OffsetDateTime.now());
            }
            cmd = cmdRepo.save(cmd);
        }

        return cmd;
    }

    @Transactional
    public void ackCommand(String deviceKey, Long commandId) {
        DeviceEntity device = deviceRepo.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new NotFoundException("Device does not exist: " + deviceKey));

        ControlCommandEntity cmd = cmdRepo.findById(commandId)
                .orElseThrow(() -> new NotFoundException("Command does not exist: " + commandId));

        if (cmd.getDevice() == null || cmd.getDevice().getId() == null
                || !cmd.getDevice().getId().equals(device.getId())) {
            throw new BadRequestException("Command does not belong to device: " + deviceKey);
        }

        if (cmd.getStatus() == CommandStatus.ACKED) {
            return;
        }

        if (cmd.getStatus() != CommandStatus.PENDING && cmd.getStatus() != CommandStatus.SENT) {
            throw new BadRequestException("Command is not ack-able: " + commandId);
        }

        if (cmd.getStatus() == CommandStatus.PENDING) {
            cmd.setStatus(CommandStatus.SENT);
            if (cmd.getSentAt() == null) {
                cmd.setSentAt(OffsetDateTime.now());
            }
        }

        cmd.setStatus(CommandStatus.ACKED);
        cmd.setAckAt(OffsetDateTime.now());
        cmdRepo.save(cmd);
        log.info("COMMAND ack id={} deviceKey={}", commandId, deviceKey);
    }

    private boolean existsPendingSame(Long deviceId, String target, Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Boolean boolValue) {
            return cmdRepo.existsPendingSameBoolean(deviceId, target, boolValue);
        }

        if (value instanceof Number numberValue) {
            return cmdRepo.existsPendingSameNumber(deviceId, target, numberValue.doubleValue());
        }

        return cmdRepo.existsPendingSameText(deviceId, target, String.valueOf(value));
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "system";
        }
        String normalized = source.trim().toLowerCase();
        return switch (normalized) {
            case "manual", "automation", "scheduler", "scene", "system" -> normalized;
            case "auto", "auto_control" -> "automation";
            case "mode_schedule" -> "scheduler";
            default -> normalized;
        };
    }
}
