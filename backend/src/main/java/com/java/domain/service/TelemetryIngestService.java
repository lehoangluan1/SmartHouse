package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.config.InvalidTelemetryException;
import com.java.controller.dto.TelemetryIngestRequest;
import com.java.eventing.DomainEventBus;
import com.java.mapper.TelemetryEventMapper;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelemetryIngestService {

    private final TelemetryPersistenceService telemetryPersistenceService;
    private final TelemetryEventMapper telemetryEventMapper;
    private final DomainEventBus eventBus;
    private final InvalidTelemetryHandler invalidTelemetryHandler;
    private final DeviceRepository deviceRepository;

    @Transactional
    public void ingest(TelemetryIngestRequest request) {
        try {
            var result = telemetryPersistenceService.persist(request);

            eventBus.publish(telemetryEventMapper.toEvent(result, request.value()));

        } catch (InvalidTelemetryException ex) {
            deviceRepository.findByDeviceKey(request.deviceKey()).ifPresent(device ->
                    invalidTelemetryHandler.handle(device, request.sensorType(), request.value(), ex.getMessage())
            );
            throw new BadRequestException(ex.getMessage());
        }
    }
}