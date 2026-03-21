package com.java.eventing;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeModeChangedEvent implements DomainEvent {
    private final Long homeId;
    private final Long deviceId;
    private final String mode;
    @Override
    public String eventType() {
        return "HOME_MODE_CHANGED";
    }
}