package com.java.eventing;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TelemetryIngestedEvent implements DomainEvent {
    private final Long homeId;
    private final Long deviceId;
    private final Object value;
    private final OffsetDateTime createdAt;
    @Override
    public String eventType(){
        return "TELEMETRY_INGESTED";
    }
}