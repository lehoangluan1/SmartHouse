package com.java.mapper;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.java.domain.service.dto.TelemetryPersistenceResult;
import com.java.eventing.TelemetryIngestedEvent;

@Component
public class TelemetryEventMapper {

    public TelemetryIngestedEvent toEvent(TelemetryPersistenceResult result, Object rawValue) {
        Object previousValue = "-";
        Object nextValue = "-";
        boolean changed = false;

        if (result != null && result.stateWriteResult() != null) {
            previousValue = result.stateWriteResult().previousValue() == null
                    ? "-"
                    : result.stateWriteResult().previousValue();

            nextValue = result.stateWriteResult().nextValue() == null
                    ? "-"
                    : result.stateWriteResult().nextValue();

            changed = result.stateWriteResult().changed();
        }

        return TelemetryIngestedEvent.builder()
                .homeId(result.device().getHome().getId())
                .deviceId(result.device().getId())
                .sensorType(result.sensorType().name())
                .rawValue(rawValue)
                .previousValue(previousValue)
                .nextValue(nextValue)
                .changed(changed)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}