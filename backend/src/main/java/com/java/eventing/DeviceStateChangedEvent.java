package com.java.eventing;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviceStateChangedEvent implements DomainEvent {
    private final Long homeId;
    private final Long deviceId;
    private final String status;
    private final Integer speed;
    private final Integer brightness;
    @Override
    public String eventType(){
        return "DEVICE_STATE_CHANGED";
    }
}