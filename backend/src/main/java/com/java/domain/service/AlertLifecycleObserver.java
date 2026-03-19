package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.domain.events.AlertLifecycleEvent;
import com.java.eventing.DomainEvent;
import com.java.eventing.DomainEventListener;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertLifecycleObserver implements DomainEventListener<AlertLifecycleEvent> {

    private final ActivityLogService activityLogService;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof AlertLifecycleEvent;
    }

    @Override
    public void onEvent(AlertLifecycleEvent event) {
        activityLogService.log(
                event.homeId(),
                event.deviceId(),
                null,
                "ALERT_" + event.action(),
                "observer",
                null,
                null,
                "{\"alertId\":" + event.alertId() + "}"
        );
    }
}