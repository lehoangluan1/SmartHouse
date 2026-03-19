package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.domain.SensorType;
import com.java.domain.service.dto.TelemetryPersistenceResult;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelemetryAutomationService {

    private final ModeAutomationService modeAutomationService;

    public void handle(TelemetryPersistenceResult result) {
        if (result.sensorType() == SensorType.TEMPERATURE || result.sensorType() == SensorType.LIGHT) {
            modeAutomationService.evaluateAllByHome(result.device().getHome().getId());
        }
    }
}