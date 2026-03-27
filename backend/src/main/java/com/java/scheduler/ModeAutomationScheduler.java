package com.java.scheduler;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.java.domain.service.DeviceAutomationDispatcher;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModeAutomationScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceAutomationDispatcher dispatcher;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 3000, initialDelay = 2000)
    public void reevaluateHomes() {
        if (!running.compareAndSet(false, true)) {
            log.info("AUTO_SCHEDULER_SKIP_ALREADY_RUNNING");
            return;
        }

        long started = System.currentTimeMillis();
        try {
            List<Long> deviceIds = deviceRepository.findAll().stream()
                    .filter(d -> d.getHome() != null && d.getHome().getId() != null)
                    .map(DeviceEntity::getId)
                    .toList();

            for (Long deviceId : deviceIds) {
                dispatcher.dispatch(deviceId);
            }
        } finally {
            log.info("AUTO_SCHEDULER_END durationMs={}", System.currentTimeMillis() - started);
            running.set(false);
        }
    }
}