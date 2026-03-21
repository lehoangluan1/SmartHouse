package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.ControlCommandRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutoControlService {

    private final ControlCommandRepository controlCommandRepository;
    private final DeviceTargetPolicy deviceTargetPolicy;
    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final ControlCommandFactory controlCommandFactory;
    private final ControlCommandSender controlCommandSender;
    private final ActivityLogService activityLogService;
    private final ActivityLogPayloadBuilder activityLogPayloadBuilder;

    @Transactional
    public boolean execute(DeviceEntity device, String target, String value, String method) {
        deviceTargetPolicy.validateAutoRequest(target, value);

        String normalizedTarget = deviceTargetPolicy.normalizeTarget(target);
        Object normalizedValue = deviceTargetPolicy.normalizeValue(normalizedTarget, value);

        deviceTargetPolicy.validateTargetForDevice(device, normalizedTarget);

        if (!deviceRuntimeStateService.hasChanged(device.getId(), normalizedTarget, normalizedValue)) {
            return false;
        }

        var command = controlCommandFactory.createSystem(device, normalizedTarget, normalizedValue);
        command = controlCommandRepository.save(command);

        DeviceRuntimeStateService.StateWriteResult stateWriteResult =
                deviceRuntimeStateService.upsertValueAndRecordHistory(
                        device.getId(),
                        normalizedTarget,
                        normalizedValue,
                        "AUTO_CONTROL",
                        command.getId(),
                        null
                );

        controlCommandSender.sendNow(command);

        activityLogService.log(
                device.getHome() != null ? device.getHome().getId() : null,
                device.getId(),
                null,
                "AUTO_CONTROL",
                method,
                null,
                null,
                activityLogPayloadBuilder.controlPayload(
                        normalizedTarget,
                        stateWriteResult.previousValue(),
                        stateWriteResult.nextValue()
                )
        );

        return true;
    }
}