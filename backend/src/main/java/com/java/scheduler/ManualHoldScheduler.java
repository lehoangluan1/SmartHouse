package com.java.scheduler;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.java.domain.service.ManualHoldService;
import com.java.persistence.repo.ActivityLogRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ManualHoldScheduler {

    private static final String ACTION_STARTED = "MANUAL_HOLD_STARTED";

    private final ActivityLogRepository activityLogRepository;
    private final ManualHoldService manualHoldService;

    @Scheduled(fixedDelay = 30000)
    public void restoreExpiredManualHolds() {
        Set<Long> deviceIds = new LinkedHashSet<>();

        activityLogRepository.findByActionOrderByCreatedAtDesc(ACTION_STARTED)
                .forEach(log -> {
                    if (log.getDevice() != null && log.getDevice().getId() != null) {
                        deviceIds.add(log.getDevice().getId());
                    }
                });

        deviceIds.forEach(manualHoldService::restoreIfExpired);
    }
}