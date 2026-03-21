package com.java.domain.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.java.persistence.entity.DeviceEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelemetryErrorAuditService {

    private final ActivityLogService activityLogService;

    public void logInvalidPayload(DeviceEntity device, String sensorType, Object rawValue, String reason) {
        if (device == null || device.getHome() == null) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sensorType", sensorType);
        payload.put("rawValue", rawValue);
        payload.put("reason", reason);

        activityLogService.log(
                device.getHome().getId(),
                device.getId(),
                null,
                "INGEST_TELEMETRY_INVALID",
                "device",
                null,
                null,
                payload
        );
    }
}