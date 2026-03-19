package com.java.domain.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.controller.dto.AlertResponse;
import com.java.controller.dto.DeviceResponse;
import com.java.mapper.DashboardAlertMapper;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.AlertRepository;
import com.java.persistence.repo.DeviceRepository;
import com.java.persistence.repo.SensorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final SensorRepository sensorRepository;

    private final DeviceRuntimeStateSnapshotReader runtimeStateSnapshotReader;
    private final DashboardDeviceViewAssembler dashboardDeviceViewAssembler;
    private final DashboardAlertMapper dashboardAlertMapper;
    private final DashboardMetricsCalculator dashboardMetricsCalculator;

    public Map<String, Object> summary(Long homeId) {
        List<DeviceEntity> devices = deviceRepository.findByHomeId(homeId);

        Map<Long, Map<String, Object>> runtimeStates = runtimeStateSnapshotReader.readByDevices(devices);

        List<DeviceResponse> deviceResponses = devices.stream()
                .map(device -> dashboardDeviceViewAssembler.toResponse(
                        device,
                        runtimeStates.getOrDefault(device.getId(), Map.of())
                ))
                .toList();

        List<AlertResponse> alertResponses = alertRepository.findByHomeIdOrderByCreatedAtDesc(homeId)
                .stream()
                .map(dashboardAlertMapper::toResponse)
                .toList();

        long sensorCount = dashboardMetricsCalculator.calculateSensorCount(devices, sensorRepository);
        long onlineDevices = dashboardMetricsCalculator.countOnlineDevices(devices);
        long autoModeDevices = dashboardMetricsCalculator.countAutoModeDevices(deviceResponses);

        Map<String, Object> data = new HashMap<>();
        data.put("deviceCount", deviceResponses.size());
        data.put("devices", deviceResponses);
        data.put("alerts", alertResponses);
        data.put("alertCount", alertResponses.size());
        data.put("sensorCount", sensorCount);
        data.put("onlineDevices", onlineDevices);
        data.put("autoModeDevices", autoModeDevices);

        return data;
    }
}