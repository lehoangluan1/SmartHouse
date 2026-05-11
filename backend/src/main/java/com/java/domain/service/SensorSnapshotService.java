package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.persistence.entity.SensorDataEntity;
import com.java.persistence.entity.SensorEntity;
import com.java.persistence.repo.SensorDataRepository;
import com.java.persistence.repo.SensorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorSnapshotService {

    private final SensorRepository sensorRepository;
    private final SensorDataRepository sensorDataRepository;

    public Double latestNumericValue(Long homeId, String sensorKind) {
        if (homeId == null || sensorKind == null || sensorKind.isBlank()) {
            return null;
        }

        SensorEntity sensor = sensorRepository.findFirstByDeviceHomeIdAndSensorKindOrderByUpdatedAtDesc(
                homeId,
                sensorKind.trim().toUpperCase()
        ).orElse(null);

        if (sensor == null || sensor.getId() == null) {
            return null;
        }

        SensorDataEntity latest = sensorDataRepository.findFirstBySensor_IdOrderByCreatedAtDesc(sensor.getId()).orElse(null);
        if (latest == null) {
            return null;
        }

        return latest.getValueNumeric();
    }

    public Double latestNumericValueForDevice(Long deviceId, String sensorKind) {
        if (deviceId == null || sensorKind == null || sensorKind.isBlank()) {
            return null;
        }

        SensorEntity sensor = sensorRepository.findByDeviceIdAndSensorKind(
                deviceId,
                sensorKind.trim().toUpperCase()
        ).orElse(null);

        if (sensor == null || sensor.getId() == null) {
            return null;
        }

        SensorDataEntity latest = sensorDataRepository.findFirstBySensor_IdOrderByCreatedAtDesc(sensor.getId()).orElse(null);
        if (latest == null) {
            return null;
        }

        return latest.getValueNumeric();
    }
}
