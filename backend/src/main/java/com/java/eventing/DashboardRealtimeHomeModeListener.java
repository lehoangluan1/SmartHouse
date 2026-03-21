package com.java.eventing;

import org.springframework.stereotype.Component;

import com.java.domain.service.DashboardRealtimeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardRealtimeHomeModeListener implements DomainEventListener<HomeModeChangedEvent> {

    private final DashboardRealtimeService dashboardRealtimeService;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof HomeModeChangedEvent;
    }

    @Override
    public void onEvent(HomeModeChangedEvent event) {
        if (event == null || event.getHomeId() == null || event.getDeviceId() == null) {
            return;
        }

        log.debug("DashboardRealtimeHomeModeListener: homeId={}, deviceId={}, mode={}",
                event.getHomeId(),
                event.getDeviceId(),
                event.getMode());

        dashboardRealtimeService.publishHomeModeChanged(
                event.getHomeId(),
                event.getDeviceId(),
                event.getMode()
        );
    }
}