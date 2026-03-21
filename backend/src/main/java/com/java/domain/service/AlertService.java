package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.config.NotFoundException;
import com.java.controller.dto.AlertResponse;
import com.java.domain.AlertStatus;
import com.java.domain.AlertType;
import com.java.domain.events.AlertLifecycleEvent;
import com.java.eventing.AlertActivatedEvent;
import com.java.eventing.DomainEventBus;
import com.java.persistence.entity.AlertEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.SensorEntity;
import com.java.persistence.repo.AlertRepository;
import com.java.persistence.repo.HomeRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final HomeRepository homeRepository;
    private final UserRepository userRepository;
    private final DomainEventBus eventBus;

    @Transactional(readOnly = true)
    public List<AlertResponse> getByHome(Long homeId) {
        return alertRepository.findByHomeIdOrderByCreatedAtDesc(homeId)
                .stream()
                .map(AlertResponse::from)
                .toList();
    }

    @Transactional
    public AlertResponse openOrRefresh(Long homeId, Long deviceId, Long sensorId, AlertType type, String message) {
        OffsetDateTime now = OffsetDateTime.now();

        AlertEntity alert = alertRepository.findTopOpen(deviceId, sensorId, type);
        boolean isNew = (alert == null);

        if (alert == null) {
            alert = new AlertEntity();
            alert.setHome(homeRepository.getReferenceById(homeId));
        }

        if (deviceId != null) {
            DeviceEntity device = new DeviceEntity();
            device.setId(deviceId);
            alert.setDevice(device);
        } else {
            alert.setDevice(null);
        }

        if (sensorId != null) {
            SensorEntity sensor = new SensorEntity();
            sensor.setId(sensorId);
            alert.setSensor(sensor);
        } else {
            alert.setSensor(null);
        }

        alert.setType(type);
        alert.setMessage(message);
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setLastTriggeredAt(now);

        AlertEntity saved = alertRepository.save(alert);

        log.info("Publishing AlertActivatedEvent: alertId={}, type={}, isNew={}",
                saved.getId(), saved.getType(), isNew);

        eventBus.publish(new AlertActivatedEvent(
                saved.getId(),
                saved.getHome() == null ? homeId : saved.getHome().getId(),
                saved.getDevice() == null ? null : saved.getDevice().getId(),
                saved.getSensor() == null ? null : saved.getSensor().getId(),
                saved.getType(),
                saved.getMessage(),
                saved.getLastTriggeredAt(),
                isNew
        ));

        return AlertResponse.from(saved);
    }

    @Transactional
    public AlertResponse acknowledge(Long alertId, Long userId) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert does not exist"));

        if (alert.getStatus() != AlertStatus.ACTIVE) {
            throw new BadRequestException("Can only acknowledge ACTIVE alerts");
        }

        alert.setStatus(AlertStatus.ACK);
        alert.setAcknowledgedBy(userRepository.getReferenceById(userId));
        alert.setAcknowledgedAt(OffsetDateTime.now());

        AlertEntity saved = alertRepository.save(alert);

        eventBus.publish(new AlertLifecycleEvent(
                saved.getId(),
                saved.getHome() == null ? null : saved.getHome().getId(),
                saved.getDevice() == null ? null : saved.getDevice().getId(),
                "ACK"
        ));

        return AlertResponse.from(saved);
    }

    @Transactional
    public AlertResponse resolve(Long alertId, Long userId) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert does not exist"));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            return AlertResponse.from(alert);
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedBy(userRepository.getReferenceById(userId));
        alert.setResolvedAt(OffsetDateTime.now());

        AlertEntity saved = alertRepository.save(alert);

        eventBus.publish(new AlertLifecycleEvent(
                saved.getId(),
                saved.getHome() == null ? null : saved.getHome().getId(),
                saved.getDevice() == null ? null : saved.getDevice().getId(),
                "RESOLVED"
        ));

        return AlertResponse.from(saved);
    }
}