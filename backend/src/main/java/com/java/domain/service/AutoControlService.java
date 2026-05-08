package com.java.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.persistence.entity.DeviceEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutoControlService {

        private static final Logger log = LoggerFactory.getLogger(ModeAutomationServiceImpl.class);
        private final DeviceTargetPolicy deviceTargetPolicy;
        private final DeviceRuntimeStateService deviceRuntimeStateService;
        private final DeviceControlService deviceControlService;
        private final ActivityLogService activityLogService;
        private final ActivityLogPayloadBuilder activityLogPayloadBuilder;
        @Transactional
        public boolean execute(DeviceEntity device, String target, String value, String method) {
        deviceTargetPolicy.validateAutoRequest(target, value);

        String normalizedTarget = deviceTargetPolicy.normalizeTarget(target);
        Object normalizedValue = deviceTargetPolicy.normalizeValue(normalizedTarget, value);

        deviceTargetPolicy.validateTargetForDevice(device, normalizedTarget);

        if (!deviceRuntimeStateService.hasChanged(device.getId(), normalizedTarget, normalizedValue)) {
                log.info("AUTO_SKIP_NO_CHANGE deviceId={}, target={}, value={}",
                        device.getId(), normalizedTarget, normalizedValue);
                return false;
        }

        DeviceControlService.ControlResult controlResult = deviceControlService.controlDevice(
                device,
                normalizedTarget,
                normalizedValue,
                "automation",
                null,
                "automation"
        );

        DeviceRuntimeStateService.StateWriteResult stateWriteResult = controlResult.stateWriteResult();

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
