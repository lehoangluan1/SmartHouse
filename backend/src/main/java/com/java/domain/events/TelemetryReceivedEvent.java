package com.java.domain.events;

import com.java.eventing.DomainEvent;

public record TelemetryReceivedEvent(Long homeId, Long deviceId, Long sensorId, String sensorType, Object value) implements DomainEvent {
    @Override
    public String eventType() {
        return "TelemetryReceivedEvent";
    }
}
