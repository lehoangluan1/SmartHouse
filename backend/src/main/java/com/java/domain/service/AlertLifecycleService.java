package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.domain.AlertStatus;
import com.java.domain.AlertType;
import com.java.domain.events.AlertLifecycleEvent;
import com.java.eventing.AlertActivatedEvent;
import com.java.eventing.DomainEventBus;
import com.java.persistence.entity.AlertEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.SensorEntity;
import com.java.persistence.repo.AlertRepository;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertLifecycleService {

    private final AlertRepository alertRepo;
    private final DeviceRepository deviceRepo;
    private final DomainEventBus eventBus;

    @Transactional
    public void upsertActiveAlert(Long deviceId, Long sensorId, AlertType type, String message) {
        if (type == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        AlertEntity existing = alertRepo.findTopOpen(deviceId, sensorId, type);

        if (existing != null) {
            OffsetDateTime previousTriggeredAt = existing.getLastTriggeredAt() != null
                    ? existing.getLastTriggeredAt()
                    : existing.getCreatedAt();

            existing.setLastTriggeredAt(now);
            if (message != null && !message.isBlank()) {
                existing.setMessage(message);
            }

            AlertEntity saved = alertRepo.save(existing);

            log.info("Publishing AlertActivatedEvent (refresh): alertId={}, type={}", saved.getId(), saved.getType());

            eventBus.publish(new AlertActivatedEvent(
                    saved.getId(),
                    saved.getHome() == null ? null : saved.getHome().getId(),
                    saved.getDevice() == null ? null : saved.getDevice().getId(),
                    saved.getSensor() == null ? null : saved.getSensor().getId(),
                    saved.getType(),
                    saved.getMessage(),
                    previousTriggeredAt,
                    false
            ));

            return;
        }

        DeviceEntity device = null;
        if (deviceId != null) {
            device = deviceRepo.findById(deviceId).orElseThrow();
        }

        AlertEntity alert = new AlertEntity();
        if (device != null) {
            alert.setHome(device.getHome());
            alert.setDevice(device);
        }

        if (sensorId != null) {
            SensorEntity sensorRef = new SensorEntity();
            sensorRef.setId(sensorId);
            alert.setSensor(sensorRef);
        }

        alert.setType(type);
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setMessage(message);
        alert.setLastTriggeredAt(now);

        AlertEntity saved = alertRepo.save(alert);

        log.info("Publishing AlertActivatedEvent (new): alertId={}, type={}", saved.getId(), saved.getType());

        eventBus.publish(new AlertActivatedEvent(
                saved.getId(),
                saved.getHome() == null ? null : saved.getHome().getId(),
                saved.getDevice() == null ? null : saved.getDevice().getId(),
                saved.getSensor() == null ? null : saved.getSensor().getId(),
                saved.getType(),
                saved.getMessage(),
                saved.getLastTriggeredAt(),
                true
        ));
    }

    @Transactional
    public void resolveIfExists(Long deviceId, Long sensorId, AlertType type) {
        AlertEntity open = alertRepo.findTopOpen(deviceId, sensorId, type);
        if (open == null) {
            return;
        }

        open.setStatus(AlertStatus.RESOLVED);
        open.setResolvedAt(OffsetDateTime.now());

        AlertEntity saved = alertRepo.save(open);

        eventBus.publish(new AlertLifecycleEvent(
                saved.getId(),
                saved.getHome() == null ? null : saved.getHome().getId(),
                saved.getDevice() == null ? null : saved.getDevice().getId(),
                "RESOLVED"
        ));
    }
}