package com.java.scheduler;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.java.domain.service.ModeAutomationService;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModeAutomationScheduler {

    private final DeviceRepository deviceRepository;
    private final ModeAutomationService modeAutomationService;

    @Scheduled(fixedDelay = 3000)
    public void reevaluateHomes() {
        Set<Long> homeIds = new LinkedHashSet<>();

        for (DeviceEntity device : deviceRepository.findAll()) {
            if (device.getHome() != null && device.getHome().getId() != null) {
                homeIds.add(device.getHome().getId());
            }
        }

        for (Long homeId : homeIds) {
            try {
                modeAutomationService.evaluateAllByHome(homeId);
            } catch (Exception e) {
                log.warn("ModeAutomationScheduler failed for homeId={}", homeId, e);
            }
        }
    }
}