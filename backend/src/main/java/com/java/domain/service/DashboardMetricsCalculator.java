package com.java.domain.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.java.controller.dto.DeviceResponse;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.SensorRepository;

@Component
public class DashboardMetricsCalculator {

    public long calculateSensorCount(List<DeviceEntity> devices, SensorRepository sensorRepository) {
        return devices.stream()
                .mapToLong(device -> sensorRepository.findByDeviceId(device.getId()).size())
                .sum();
    }

    public long countOnlineDevices(List<DeviceEntity> devices) {
        return devices.stream()
                .filter(device -> Boolean.TRUE.equals(device.getIsOnline()))
                .count();
    }

    public long countAutoModeDevices(List<DeviceResponse> deviceResponses) {
        return deviceResponses.stream()
                .filter(device -> "auto".equalsIgnoreCase(device.getMode()))
                .count();
    }
}