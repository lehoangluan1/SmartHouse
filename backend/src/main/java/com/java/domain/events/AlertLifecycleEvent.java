package com.java.domain.events;

import com.java.eventing.DomainEvent;

public record AlertLifecycleEvent(Long alertId, Long homeId, Long deviceId, String action) implements DomainEvent {
    @Override
    public String eventType() {
        return "AlertLifecycleEvent";
    }
}
