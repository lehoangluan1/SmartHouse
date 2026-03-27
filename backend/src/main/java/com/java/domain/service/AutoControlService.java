package com.java.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.ControlCommandRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutoControlService {

        private static final Logger log = LoggerFactory.getLogger(ModeAutomationServiceImpl.class);
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

        if (!deviceRuntimeStateService.hasChanged(device.getId(), normalizedTarget, normalizedValue)) {
                log.info("AUTO_SKIP_NO_CHANGE deviceId={}, target={}, value={}",
                        device.getId(), normalizedTarget, normalizedValue);
                return false;
        }

        command = controlCommandSender.sendNow(command);

        if (command.getStatus() != com.java.domain.CommandStatus.SENT) {
                log.warn("AUTO_SEND_FAILED deviceId={}, target={}, value={}, status={}",
                        device.getId(), normalizedTarget, normalizedValue, command.getStatus());
                return false;
        }

        if (command.getStatus() != com.java.domain.CommandStatus.SENT) {
                return false;
        }

        DeviceRuntimeStateService.StateWriteResult stateWriteResult =
                deviceRuntimeStateService.upsertValueAndRecordHistory(
                        device.getId(),
                        normalizedTarget,
                        normalizedValue,
                        "AUTO_CONTROL",
                        command.getId(),
                        null
                );

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