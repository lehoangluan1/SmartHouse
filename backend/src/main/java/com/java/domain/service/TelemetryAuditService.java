package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.java.eventing.TelemetryIngestedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelemetryAuditService {

    private final ActivityLogService activityLogService;
    private final ActivityLogPayloadBuilder activityLogPayloadBuilder;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIngest(TelemetryIngestedEvent event) {
        activityLogService.log(
                event.getHomeId(),
                event.getDeviceId(),
                null,
                "INGEST_TELEMETRY",
                "device",
                null,
                null,
                activityLogPayloadBuilder.telemetryPayload(
                        event.getSensorType(),
                        event.getPreviousValue(),
                        event.getNextValue(),
                        event.getRawValue()
                )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIngestWithoutStateChange(TelemetryIngestedEvent event) {
        activityLogService.log(
                event.getHomeId(),
                event.getDeviceId(),
                null,
                "INGEST_TELEMETRY_NO_STATE_CHANGE",
                "device",
                null,
                null,
                activityLogPayloadBuilder.telemetryPayload(
                        event.getSensorType(),
                        event.getPreviousValue(),
                        event.getNextValue(),
                        event.getRawValue()
                )
        );
    }
}