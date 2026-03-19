package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.domain.AlertStatus;
import com.java.domain.AlertType;
import com.java.domain.events.AlertLifecycleEvent;
import com.java.eventing.AlertNotificationEvent;
import com.java.eventing.DomainEventBus;
import com.java.persistence.entity.AlertEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.SensorEntity;
import com.java.persistence.repo.AlertRepository;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertLifecycleService {

    private final AlertRepository alertRepo;
    private final DeviceRepository deviceRepo;
    private final DomainEventBus eventBus;
    private final TelegramAlertPolicy telegramAlertPolicy;

    @Transactional
    public void upsertActiveAlert(Long deviceId, Long sensorId, AlertType type, String message) {
        AlertEntity existing = alertRepo.findTopOpen(deviceId, sensorId, type);
        OffsetDateTime now = OffsetDateTime.now();

        if (existing != null) {
            existing.setLastTriggeredAt(now);
            if (message != null && !message.isBlank()) {
                existing.setMessage(message);
            }
            AlertEntity saved = alertRepo.save(existing);

            eventBus.publish(new AlertLifecycleEvent(
                    saved.getId(),
                    saved.getHome() == null ? null : saved.getHome().getId(),
                    saved.getDevice() == null ? null : saved.getDevice().getId(),
                    "ACTIVE"
            ));

            if (telegramAlertPolicy.shouldNotify(saved.getType())) {
                eventBus.publish(new AlertNotificationEvent(
                        saved.getId(),
                        saved.getHome() == null ? null : saved.getHome().getId(),
                        saved.getDevice() == null ? null : saved.getDevice().getId(),
                        saved.getSensor() == null ? null : saved.getSensor().getId(),
                        saved.getType(),
                        saved.getMessage(),
                        saved.getLastTriggeredAt(),
                        false
                ));
            }

            return;
        }

        DeviceEntity device = deviceRepo.findById(deviceId).orElseThrow();

        AlertEntity alert = new AlertEntity();
        alert.setHome(device.getHome());
        alert.setDevice(device);

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

        eventBus.publish(new AlertLifecycleEvent(
                saved.getId(),
                saved.getHome() == null ? null : saved.getHome().getId(),
                saved.getDevice() == null ? null : saved.getDevice().getId(),
                "ACTIVE"
        ));

        if (telegramAlertPolicy.shouldNotify(type)) {
            eventBus.publish(new AlertNotificationEvent(
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