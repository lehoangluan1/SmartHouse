package com.java.scheduler;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.java.domain.service.DashboardRealtimeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DashboardRealtimeHeartbeatJob {

    private final DashboardRealtimeService dashboardRealtimeService;

    @Scheduled(fixedDelay = 20000)
    public void heartbeat() {
        dashboardRealtimeService.heartbeatAll();
    }
 
}
