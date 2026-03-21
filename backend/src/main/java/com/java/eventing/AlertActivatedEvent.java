package com.java.eventing;

import java.time.OffsetDateTime;

import com.java.domain.AlertType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class AlertActivatedEvent implements DomainEvent {

    public static final String EVENT_TYPE = "ALERT_ACTIVATED";

    private final Long alertId;
    private final Long homeId;
    private final Long deviceId;
    private final Long sensorId;
    private final AlertType type;
    private final String message;
    private final OffsetDateTime triggeredAt;
    private final boolean newlyCreated;

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}