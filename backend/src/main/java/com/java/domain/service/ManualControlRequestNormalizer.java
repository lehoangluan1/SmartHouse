package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.DeviceEntity;
import com.java.controller.dto.ControlRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ManualControlRequestNormalizer {

    private final DeviceTargetPolicy deviceTargetPolicy;

    public NormalizedControlRequest normalize(DeviceEntity device, ControlRequest request) {
        deviceTargetPolicy.validateManualRequest(request);

        String normalizedTarget = deviceTargetPolicy.normalizeTarget(request.target());
        Object normalizedValue = deviceTargetPolicy.normalizeValue(normalizedTarget, request.value());

        deviceTargetPolicy.validateTargetForDevice(device, normalizedTarget);

        return new NormalizedControlRequest(normalizedTarget, normalizedValue);
    }
}