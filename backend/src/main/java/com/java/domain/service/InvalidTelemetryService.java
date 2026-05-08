package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.InvalidTelemetryException;
import com.java.controller.dto.TelemetryIngestRequest;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvalidTelemetryService {

    private final DeviceRepository deviceRepository;
    private final InvalidTelemetryHandler invalidTelemetryHandler;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(TelemetryIngestRequest request, InvalidTelemetryException ex) {
        deviceRepository.findByDeviceKey(request.deviceKey()).ifPresent(device ->
                invalidTelemetryHandler.handle(
                        device,
                        request.sensorType(),
                        request.value(),
                        ex.getMessage()
                )
        );
    }
}