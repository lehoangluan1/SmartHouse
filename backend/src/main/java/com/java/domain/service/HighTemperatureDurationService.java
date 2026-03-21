package com.java.domain.service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.domain.AlertType;
import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.DeviceConfigEntity;
import com.java.persistence.entity.SensorDataEntity;
import com.java.persistence.entity.SensorEntity;
import com.java.persistence.repo.DeviceConfigRepository;
import com.java.persistence.repo.SensorDataRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HighTemperatureDurationService {

    private final DeviceConfigRepository deviceConfigRepository;
    private final SensorDataRepository sensorDataRepo;
    private final AlertLifecycleService alertLifecycleService;

    @Transactional
    public void evaluate(Long deviceId, SensorDataEntity latest) {
        if (latest == null || latest.getSensor() == null) {
            return;
        }

        SensorEntity sensor = latest.getSensor();
        if (!"TEMPERATURE".equalsIgnoreCase(sensor.getSensorKind())) {
            return;
        }

        DeviceConfigEntity deviceConfig = deviceConfigRepository.findLatestByDeviceId(deviceId);
        if (deviceConfig == null || deviceConfig.getConfig() == null) {
            return;
        }

        ConfigEntity cfg = deviceConfig.getConfig();
        if (cfg.getThigh() == null || cfg.getNMinutes() == null) {
            return;
        }

        Double currentTemp = latest.getValueNumeric();
        if (currentTemp == null) {
            return;
        }

        double thigh = cfg.getThigh();
        int nMinutes = cfg.getNMinutes();

        if (currentTemp <= thigh) {
            alertLifecycleService.resolveIfExists(deviceId, sensor.getId(), AlertType.HIGH_TEMPERATURE);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime since = now.minus(nMinutes, ChronoUnit.MINUTES);

        List<SensorDataEntity> list =
                sensorDataRepo.findBySensor_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(sensor.getId(), since);

        if (list == null || list.isEmpty()) {
            return;
        }

        int samplePeriodSec = 15;
        long maxGapSec = 2L * samplePeriodSec + 5;

        list.sort(Comparator.comparing(SensorDataEntity::getCreatedAt));

        boolean continuous = true;
        OffsetDateTime firstAt = null;
        OffsetDateTime prevAt = null;
        OffsetDateTime lastAt = null;

        for (SensorDataEntity item : list) {
            OffsetDateTime at = item.getCreatedAt();
            Double temp = item.getValueNumeric();

            if (at == null || temp == null || temp <= thigh) {
                continuous = false;
                break;
            }

            if (prevAt == null) {
                firstAt = at;
            } else {
                long gap = ChronoUnit.SECONDS.between(prevAt, at);
                if (gap > maxGapSec) {
                    continuous = false;
                    break;
                }
            }

            prevAt = at;
            lastAt = at;
        }

        if (continuous && firstAt != null && lastAt != null) {
            long coveredSec = ChronoUnit.SECONDS.between(firstAt, lastAt);
            long requiredSec = nMinutes * 60L;
            if (coveredSec < requiredSec - maxGapSec) {
                continuous = false;
            }
        }

        if (continuous) {
            alertLifecycleService.upsertActiveAlert(
                    deviceId,
                    sensor.getId(),
                    AlertType.HIGH_TEMPERATURE,
                    "Temperature > Thigh (" + thigh + ") for >= " + nMinutes + " minutes"
            );
        }
    }
}