package com.java.eventing;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.java.domain.events.TelemetryReceivedEvent;
import com.java.domain.service.DashboardRealtimeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardRealtimeTelemetryListener implements DomainEventListener<TelemetryReceivedEvent> {

    private final DashboardRealtimeService dashboardRealtimeService;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof TelemetryReceivedEvent;
    }

    @Override
    public void onEvent(TelemetryReceivedEvent event) {
        if (event == null || event.homeId() == null || event.deviceId() == null) {
            return;
        }

        Object rawValue = event.value();

        log.debug(
                "DashboardRealtimeTelemetryListener received telemetry: homeId={}, deviceId={}, sensorId={}, sensorType={}, rawValue={}",
                event.homeId(),
                event.deviceId(),
                event.sensorId(),
                event.sensorType(),
                rawValue
        );

        dashboardRealtimeService.publishTelemetryReceived(
                event.homeId(),
                event.deviceId(),
                toDouble(rawValue),
                toText(rawValue),
                toBoolean(rawValue),
                OffsetDateTime.now()
        );
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof String text) {
            try {
                return Double.valueOf(text.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String text) {
            return text;
        }

        return null;
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof Number number) {
            return number.intValue() != 0;
        }

        if (value instanceof String text) {
            String normalized = text.trim().toLowerCase();
            if ("true".equals(normalized) || "on".equals(normalized) || "yes".equals(normalized) || "motion".equals(normalized) || "detected".equals(normalized) || "1".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized) || "off".equals(normalized) || "no".equals(normalized) || "0".equals(normalized)) {
                return false;
            }
        }

        return null;
    }
}