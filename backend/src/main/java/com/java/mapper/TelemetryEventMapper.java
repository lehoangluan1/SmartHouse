package com.java.mapper;

import org.springframework.stereotype.Component;

import com.java.domain.events.TelemetryReceivedEvent;
import com.java.domain.service.dto.TelemetryPersistenceResult;

@Component
public class TelemetryEventMapper {

    public TelemetryReceivedEvent toEvent(TelemetryPersistenceResult result, Object rawValue) {
        return new TelemetryReceivedEvent(
                result.device().getHome().getId(),
                result.device().getId(),
                result.sensor().getId(),
                result.sensorType().name(),
                rawValue
        );
    }
}