package com.java.domain.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.controller.dto.ConfigMonitoringSlotsDto;
import com.java.controller.dto.ConfigThresholdsDto;
import com.java.controller.dto.ConfigUpsertRequest;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConfigValidator {

    private final DeviceRepository deviceRepository;

    public void validate(Long homeId, ConfigUpsertRequest request) {
        if (request == null) {
            throw new BadRequestException("Invalid request");
        }

        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new BadRequestException("Config name cannot be empty");
        }

        validateThresholds(request.thresholds());
        validateMonitoringSlots(homeId, request.monitoringSlots());
    }

    private void validateThresholds(ConfigThresholdsDto thresholds) {
        if (thresholds == null) {
            throw new BadRequestException("Thresholds cannot be empty");
        }
    
        if (thresholds.tHigh() != null && thresholds.tLow() != null
                && thresholds.tHigh() <= thresholds.tLow()) {
            throw new BadRequestException("T_high must be greater than T_low");
        }
    
        if (thresholds.lHigh() != null && thresholds.lLow() != null
                && thresholds.lHigh() <= thresholds.lLow()) {
            throw new BadRequestException("L_high must be greater than L_low");
        }
    
        if (thresholds.tSleepHigh() != null && thresholds.tSleepLow() != null
                && thresholds.tSleepHigh() <= thresholds.tSleepLow()) {
            throw new BadRequestException("T_sleep_high must be greater than T_sleep_low");
        }
    
        validatePositive(thresholds.n(), "N");
        validatePositive(thresholds.m(), "M");
        validatePositive(thresholds.tHold(), "T_hold");
        validatePositive(thresholds.dPresent(), "D_present");
        validatePositive(thresholds.k(), "K");
    
        validatePercent(thresholds.autoFanSpeed(), "Auto fan speed");
        validatePercent(thresholds.sleepFanSpeed(), "Sleep fan speed");
        validatePercent(thresholds.awayFanSpeed(), "Away fan speed");
    }
    
    private void validatePositive(Integer value, String field) {
        if (value != null && value <= 0) {
            throw new BadRequestException(field + " must be greater than 0");
        }
    }
    
    private void validatePercent(Integer value, String field) {
        if (value != null && (value < 0 || value > 100)) {
            throw new BadRequestException(field + " must be between 0-100");
        }
    }

    private void validateMonitoringSlots(Long homeId, ConfigMonitoringSlotsDto slots) {
        if (slots == null) {
            throw new BadRequestException("Monitoring slots cannot be empty");
        }

        List<Long> ids = new ArrayList<>();
        addIfNotNull(ids, slots.temperatureDeviceId());
        addIfNotNull(ids, slots.humidityDeviceId());
        addIfNotNull(ids, slots.lightDeviceId());
        addIfNotNull(ids, slots.motionDeviceId());

        Set<Long> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new BadRequestException("Monitoring slots cannot have duplicate devices");
        }

        for (Long deviceId : unique) {
            DeviceEntity device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new BadRequestException("Device does not exist: " + deviceId));

            if (device.getHome() == null || !homeId.equals(device.getHome().getId())) {
                throw new BadRequestException("Device does not belong to current home: " + deviceId);
            }
        }
    }

    private void addIfNotNull(List<Long> ids, Long value) {
        if (value != null) ids.add(value);
    }
}