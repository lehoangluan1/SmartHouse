package com.java.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.java.domain.service.DeviceOfflineMonitorService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OfflineScheduler {

    private final DeviceOfflineMonitorService deviceOfflineMonitorService;

    @Scheduled(fixedDelay = 30000)
    public void syncOfflineStates() {
        deviceOfflineMonitorService.syncOfflineStates();
    }
}