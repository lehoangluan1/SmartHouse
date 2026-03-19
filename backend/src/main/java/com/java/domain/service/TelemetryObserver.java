package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.domain.events.TelemetryReceivedEvent;
import com.java.eventing.DomainEvent;
import com.java.eventing.DomainEventListener;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TelemetryObserver implements DomainEventListener<TelemetryReceivedEvent> {

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof TelemetryReceivedEvent;
    }

    @Override
    public void onEvent(TelemetryReceivedEvent event) {
        // no log
        // activityLogService.log(
        //         event.homeId(),
        //         event.deviceId(),
        //         null,
        //         "TELEMETRY_OBSERVED",
        //         "observer",
        //         null,
        //         null,
        //         "{\"sensorId\":" + event.sensorId() + "}"
        // );
    }
}