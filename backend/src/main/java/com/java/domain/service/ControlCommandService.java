package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class ControlCommandService {

    private final DeviceRepository deviceRepo;
    private final ControlCommandRepository cmdRepo;
    private final DomainEventBus bus;
    private final DeviceTargetPolicy deviceTargetPolicy;
    private final ControlCommandMapper controlCommandMapper;
    private final ControlCommandFactory controlCommandFactory;

    public ControlCommandService(
            DeviceRepository deviceRepo,
            ControlCommandRepository cmdRepo,
            DomainEventBus bus,
            DeviceTargetPolicy deviceTargetPolicy,
            ControlCommandMapper controlCommandMapper,
            ControlCommandFactory controlCommandFactory
    ) {
        this.deviceRepo = deviceRepo;
        this.cmdRepo = cmdRepo;
        this.bus = bus;
        this.deviceTargetPolicy = deviceTargetPolicy;
        this.controlCommandMapper = controlCommandMapper;
        this.controlCommandFactory = controlCommandFactory;
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

        ControlCommandEntity command = controlCommandFactory.createWithActorName(
                device,
                normalizedTarget,
                normalizedValue,
                actor
        );

        command = cmdRepo.save(command);

        bus.publish(new ControlCommandEvent(
                command.getId(),
                device.getHome() != null ? device.getHome().getId() : null,
                device.getId(),
                device.getDeviceKey(),
                normalizedTarget,
                normalizedValue,
                actor
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
        DeviceEntity device = deviceRepo.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new NotFoundException("Device does not exist: " + deviceKey));

        ControlCommandEntity cmd = cmdRepo.findNextPending(device.getId());
        if (cmd == null) {
            return null;
        }

        cmd.setStatus(CommandStatus.SENT);
        cmd.setSentAt(OffsetDateTime.now());
        cmdRepo.save(cmd);

        return controlCommandMapper.toNextResponse(cmd);
    }

    @Transactional
    public void ackCommand(String deviceKey, Long commandId) {
        DeviceEntity device = deviceRepo.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new NotFoundException("Device does not exist: " + deviceKey));

        ControlCommandEntity cmd = cmdRepo.findById(commandId)
                .orElseThrow(() -> new NotFoundException("Command does not exist: " + commandId));

        if (cmd.getDevice() == null || cmd.getDevice().getId() == null
                || !cmd.getDevice().getId().equals(device.getId())) {
            throw new IllegalArgumentException("Command does not belong to device: " + deviceKey);
        }

        cmd.setStatus(CommandStatus.ACKED);
        cmd.setAckAt(OffsetDateTime.now());
        cmdRepo.save(cmd);
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
}