package com.java.eventing;

import org.springframework.stereotype.Component;

import com.java.domain.service.DashboardRealtimeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardRealtimeDeviceStateListener implements DomainEventListener<DeviceStateChangedEvent> {

    private final DashboardRealtimeService dashboardRealtimeService;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof DeviceStateChangedEvent;
    }

    @Override
    public void onEvent(DeviceStateChangedEvent event) {
        if (event == null || event.getHomeId() == null || event.getDeviceId() == null) {
            return;
        }

        log.debug("DashboardRealtimeDeviceStateListener: homeId={}, deviceId={}, status={}, speed={}, brightness={}",
                event.getHomeId(),
                event.getDeviceId(),
                event.getStatus(),
                event.getSpeed(),
                event.getBrightness());

        dashboardRealtimeService.publishDeviceStateChanged(
                event.getHomeId(),
                event.getDeviceId(),
                event.getStatus(),
                event.getSpeed(),
                event.getBrightness()
        );
    }
}