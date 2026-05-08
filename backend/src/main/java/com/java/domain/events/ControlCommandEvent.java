package com.java.domain.events;

import com.java.eventing.DomainEvent;

public record ControlCommandEvent(
        Long commandId,
        Long homeId,
        Long deviceId,
        String deviceKey,
        String target,
        Object value,
        String source
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ControlCommandRequested";
    }
}
