package com.java.domain.service;

import com.java.controller.dto.ConfigMonitoringSlotsDto;
import com.java.controller.dto.ConfigThresholdsDto;
import com.java.controller.dto.ConfigUpsertRequest;
import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.repo.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfigUpdater {

    private final DeviceRepository deviceRepository;

    public void apply(
            ConfigEntity entity,
            HomeEntity home,
            ConfigUpsertRequest request
    ) {
        entity.setHome(home);
        entity.setName(request.name().trim());

        applyThresholds(entity, request.thresholds());
        applyMonitoringSlots(entity, request.monitoringSlots());
    }

    private void applyThresholds(ConfigEntity entity, ConfigThresholdsDto dto) {
        entity.setThigh(dto.tHigh());
        entity.setTlow(dto.tLow());
        entity.setLlow(dto.lLow());
        entity.setLhigh(dto.lHigh());
        entity.setTsleepHigh(dto.tSleepHigh());
        entity.setTsleepLow(dto.tSleepLow());
        entity.setTawayHigh(dto.tAwayHigh());
        entity.setTcritical(dto.tCritical());
        entity.setNMinutes(dto.n());
        entity.setMMinutes(dto.m());
        entity.setTholdMinutes(dto.tHold());
        entity.setDpresent(dto.dPresent());
        entity.setKMinutes(dto.k());
        entity.setAutoFanSpeed(dto.autoFanSpeed());
        entity.setSleepFanSpeed(dto.sleepFanSpeed());
        entity.setAwayFanSpeed(dto.awayFanSpeed());
    }

    private void applyMonitoringSlots(ConfigEntity entity, ConfigMonitoringSlotsDto dto) {
        entity.setMonitoringTemperatureDevice(getDevice(dto.temperatureDeviceId()));
        entity.setMonitoringHumidityDevice(getDevice(dto.humidityDeviceId()));
        entity.setMonitoringLightSensorDevice(getDevice(dto.lightSensorDeviceId()));
        entity.setMonitoringMotionDevice(getDevice(dto.motionDeviceId()));
        entity.setMonitoringFanDevice(getDevice(dto.fanDeviceId()));
        entity.setMonitoringLightDevice(getDevice(dto.lightDeviceId()));
    }

    private DeviceEntity getDevice(Long deviceId) {
        if (deviceId == null) return null;
        return deviceRepository.getReferenceById(deviceId);
    }
}