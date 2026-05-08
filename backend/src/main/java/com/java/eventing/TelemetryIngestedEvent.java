package com.java.eventing;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TelemetryIngestedEvent implements DomainEvent {
    private final Long homeId;
    private final Long deviceId;
    private final String sensorType;
    private final Object rawValue;
    private final Object previousValue;
    private final Object nextValue;
    private final boolean changed;
    private final OffsetDateTime createdAt;
    
    @Override 
    public String eventType(){
        return "TELEMETRY_INGESTED_EVENT";
    }
} 