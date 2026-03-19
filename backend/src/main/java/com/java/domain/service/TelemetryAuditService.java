package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.domain.service.dto.TelemetryPersistenceResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelemetryAuditService {

    private final ActivityLogService activityLogService;
    private final ActivityLogPayloadBuilder activityLogPayloadBuilder;

    public void logIngest(TelemetryPersistenceResult result, Object rawValue) {
        activityLogService.log(
                result.device().getHome().getId(),
                result.device().getId(),
                null,
                "INGEST_TELEMETRY",
                "device",
                null,
                null,
                activityLogPayloadBuilder.telemetryPayload(
                        result.sensorType().name(),
                        readFromState(result),
                        readToState(result),
                        rawValue
                )
        );
    }

    public void logIngestWithoutStateChange(TelemetryPersistenceResult result, Object rawValue) {
        activityLogService.log(
                result.device().getHome().getId(),
                result.device().getId(),
                null,
                "INGEST_TELEMETRY_NO_STATE_CHANGE",
                "device",
                null,
                null,
                activityLogPayloadBuilder.telemetryPayload(
                        result.sensorType().name(),
                        readFromState(result),
                        readToState(result),
                        rawValue
                )
        );
    }

    private Object readFromState(TelemetryPersistenceResult result) {
        if (result == null || result.stateWriteResult() == null) {
            return "-";
        }
        Object value = result.stateWriteResult().previousValue();
        return value == null ? "-" : value;
    }

    private Object readToState(TelemetryPersistenceResult result) {
        if (result == null || result.stateWriteResult() == null) {
            return "-";
        }
        Object value = result.stateWriteResult().nextValue();
        return value == null ? "-" : value;
    }
}