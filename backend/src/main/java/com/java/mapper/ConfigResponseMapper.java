package com.java.mapper;

import com.java.controller.dto.ConfigMonitoringSlotsDto;
import com.java.controller.dto.ConfigResponse;
import com.java.controller.dto.ConfigThresholdsDto;
import com.java.persistence.entity.ConfigEntity;
import org.springframework.stereotype.Component;

@Component
public class ConfigResponseMapper {

    public ConfigResponse toResponse(ConfigEntity entity) {
        if (entity == null) {
            return null;
        }

        return new ConfigResponse(
                entity.getId(),
                entity.getHome() != null ? entity.getHome().getId() : null,
                entity.getName(),
                entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null,
                Boolean.TRUE.equals(entity.getIsActive()),
                new ConfigThresholdsDto(
                        entity.getThigh(),
                        entity.getTlow(),
                        entity.getLlow(),
                        entity.getLhigh(),
                        entity.getTsleepHigh(),
                        entity.getTsleepLow(),
                        entity.getTawayHigh(),
                        entity.getTcritical(),
                        entity.getNMinutes(),
                        entity.getMMinutes(),
                        entity.getTholdMinutes(),
                        entity.getDpresent(),
                        entity.getKMinutes(),
                        entity.getAutoFanSpeed(),
                        entity.getSleepFanSpeed(),
                        entity.getAwayFanSpeed()
                ),
                new ConfigMonitoringSlotsDto(
                        entity.getMonitoringTemperatureDevice() != null
                                ? entity.getMonitoringTemperatureDevice().getId()
                                : null,
                        entity.getMonitoringHumidityDevice() != null
                                ? entity.getMonitoringHumidityDevice().getId()
                                : null,
                        entity.getMonitoringLightSensorDevice() != null
                                ? entity.getMonitoringLightSensorDevice().getId()
                                : null,
                        entity.getMonitoringMotionDevice() != null
                                ? entity.getMonitoringMotionDevice().getId()
                                : null,
                        entity.getMonitoringFanDevice() != null
                                ? entity.getMonitoringFanDevice().getId()
                                : null,
                        entity.getMonitoringLightDevice() != null
                                ? entity.getMonitoringLightDevice().getId()
                                : null
                ),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}