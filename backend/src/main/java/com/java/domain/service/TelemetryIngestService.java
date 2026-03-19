package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.controller.dto.TelemetryIngestRequest;
import com.java.eventing.DomainEventBus;
import com.java.mapper.TelemetryEventMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelemetryIngestService {

    private final TelemetryPersistenceService telemetryPersistenceService;
    private final TelemetryAlertService telemetryAlertService;
    private final TelemetryAutomationService telemetryAutomationService;
    private final TelemetryAuditService telemetryAuditService;
    private final TelemetryEventMapper telemetryEventMapper;
    private final DomainEventBus eventBus;

    @Transactional
    public void ingest(TelemetryIngestRequest request) {
        var result = telemetryPersistenceService.persist(request);

        eventBus.publish(telemetryEventMapper.toEvent(result, request.value()));
        telemetryAlertService.evaluateThresholds(result, request.value());
        telemetryAutomationService.handle(result);

        if (!result.stateWriteResult().changed()) {
            telemetryAuditService.logIngestWithoutStateChange(result, request.value());
        }
    }
}