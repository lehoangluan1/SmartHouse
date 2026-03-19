package com.java.domain.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ActivityLogPayloadBuilder {

    public Map<String, Object> controlPayload(String target, Object value) {
        return controlPayload(target, "-", value);
    }

    public Map<String, Object> controlPayload(String target, Object fromState, Object toState) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", normalizeTarget(target));
        payload.put("fromState", normalizeState(fromState));
        payload.put("toState", normalizeState(toState));
        payload.put("details", normalizeTarget(target) + " changed");
        return payload;
    }

    public Map<String, Object> telemetryPayload(
            String sensorType,
            Object fromState,
            Object toState,
            Object rawValue
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", normalizeTarget(sensorType));
        payload.put("fromState", normalizeState(fromState));
        payload.put("toState", normalizeState(toState));
        payload.put("rawValue", rawValue == null ? "-" : rawValue);
        payload.put("details", normalizeTarget(sensorType) + " telemetry received");
        return payload;
    }

    public Map<String, Object> telemetryPayload(String sensorType, Object rawValue) {
        return telemetryPayload(sensorType, "-", rawValue, rawValue);
    }

    private Object normalizeState(Object value) {
        return value == null ? "-" : value;
    }

    private String normalizeTarget(String target) {
        return target == null ? "UNKNOWN" : target.trim().toUpperCase();
    }
}