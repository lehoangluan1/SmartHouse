package com.java.domain.service;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.java.controller.dto.ScheduleUpsertRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleValueResolver {

    private final DeviceTargetPolicy deviceTargetPolicy;

    public String resolveCapabilityCode(ScheduleUpsertRequest request) {
        if (request == null) {
            return null;
        }

        if (request.capabilityCode() != null && !request.capabilityCode().isBlank()) {
            return deviceTargetPolicy.normalizeTarget(request.capabilityCode());
        }

        if (request.mode() != null && !request.mode().isBlank()) {
            return "MODE";
        }

        return null;
    }

    public ScheduleTypedValue resolveTypedValue(ScheduleUpsertRequest request) {
        String capabilityCode = resolveCapabilityCode(request);
        String rawValue = resolveRawValue(request);

        if (capabilityCode == null || rawValue == null || rawValue.isBlank()) {
            return new ScheduleTypedValue(null, null, null);
        }

        Object normalizedValue = deviceTargetPolicy.normalizeValue(capabilityCode, rawValue);

        if (normalizedValue instanceof Boolean booleanValue) {
            return new ScheduleTypedValue(booleanValue, null, null);
        }

        if (normalizedValue instanceof Number numberValue) {
            return new ScheduleTypedValue(null, numberValue.doubleValue(), null);
        }

        return new ScheduleTypedValue(null, null, normalizedValue.toString().toLowerCase(Locale.ROOT));
    }

    private String resolveRawValue(ScheduleUpsertRequest request) {
        if (request == null) {
            return null;
        }

        if (request.value() != null && !request.value().isBlank()) {
            return request.value().trim();
        }

        if (request.mode() != null && !request.mode().isBlank()) {
            return request.mode().trim();
        }

        return null;
    }
}