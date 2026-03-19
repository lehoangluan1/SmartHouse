package com.java.eventing;
import java.time.OffsetDateTime;

import com.java.domain.AlertType;

public class AlertNotificationEvent implements DomainEvent {

    public static final String EVENT_TYPE = "ALERT_NOTIFICATION";

    private final Long alertId;
    private final Long homeId;
    private final Long deviceId;
    private final Long sensorId;
    private final AlertType type;
    private final String message;
    private final OffsetDateTime triggeredAt;
    private final boolean newlyCreated;

    public AlertNotificationEvent(
            Long alertId,
            Long homeId,
            Long deviceId,
            Long sensorId,
            AlertType type,
            String message,
            OffsetDateTime triggeredAt,
            boolean newlyCreated
    ) {
        this.alertId = alertId;
        this.homeId = homeId;
        this.deviceId = deviceId;
        this.sensorId = sensorId;
        this.type = type;
        this.message = message;
        this.triggeredAt = triggeredAt;
        this.newlyCreated = newlyCreated;
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    public Long getAlertId() {
        return alertId;
    }

    public Long getHomeId() {
        return homeId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getSensorId() {
        return sensorId;
    }

    public AlertType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }
}