package com.java.mapper;

import com.java.persistence.entity.ConfigEntity;
import org.springframework.stereotype.Component;

@Component
public class ConfigSnapshotFactory {

    public ConfigEntity copy(ConfigEntity source) {
        if (source == null) {
            return null;
        }

        ConfigEntity copy = new ConfigEntity();
        copy.setId(source.getId());
        copy.setHome(source.getHome());
        copy.setName(source.getName());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setIsActive(source.getIsActive());

        copy.setThigh(source.getThigh());
        copy.setTlow(source.getTlow());
        copy.setLhigh(source.getLhigh());
        copy.setLlow(source.getLlow());
        copy.setTsleepHigh(source.getTsleepHigh());
        copy.setTsleepLow(source.getTsleepLow());
        copy.setTawayHigh(source.getTawayHigh());
        copy.setTcritical(source.getTcritical());
        copy.setNMinutes(source.getNMinutes());
        copy.setMMinutes(source.getMMinutes());
        copy.setTholdMinutes(source.getTholdMinutes());
        copy.setDpresent(source.getDpresent());
        copy.setKMinutes(source.getKMinutes());
        copy.setAutoFanSpeed(source.getAutoFanSpeed());
        copy.setSleepFanSpeed(source.getSleepFanSpeed());
        copy.setAwayFanSpeed(source.getAwayFanSpeed());

        copy.setMonitoringTemperatureDevice(source.getMonitoringTemperatureDevice());
        copy.setMonitoringHumidityDevice(source.getMonitoringHumidityDevice());
        copy.setMonitoringLightDevice(source.getMonitoringLightDevice());
        copy.setMonitoringMotionDevice(source.getMonitoringMotionDevice());

        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());

        return copy;
    }
}