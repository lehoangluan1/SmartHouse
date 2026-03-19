package com.java.mapper;

import com.java.persistence.entity.ConfigEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ConfigAuditMapper {

    public Map<String, Object> toSnapshot(ConfigEntity entity) {
        if (entity == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("name", entity.getName());
        map.put("isActive", entity.getIsActive());

        map.put("thigh", entity.getThigh());
        map.put("tlow", entity.getTlow());
        map.put("lhigh", entity.getLhigh());
        map.put("llow", entity.getLlow());
        map.put("tsleepHigh", entity.getTsleepHigh());
        map.put("tsleepLow", entity.getTsleepLow());
        map.put("tawayHigh", entity.getTawayHigh());
        map.put("tcritical", entity.getTcritical());
        map.put("nMinutes", entity.getNMinutes());
        map.put("mMinutes", entity.getMMinutes());
        map.put("tholdMinutes", entity.getTholdMinutes());
        map.put("dpresent", entity.getDpresent());
        map.put("kMinutes", entity.getKMinutes());
        map.put("autoFanSpeed", entity.getAutoFanSpeed());
        map.put("sleepFanSpeed", entity.getSleepFanSpeed());
        map.put("awayFanSpeed", entity.getAwayFanSpeed());

        map.put("monitoringTemperatureDeviceId",
                entity.getMonitoringTemperatureDevice() != null ? entity.getMonitoringTemperatureDevice().getId() : null);
        map.put("monitoringTemperatureDeviceName",
                entity.getMonitoringTemperatureDevice() != null ? entity.getMonitoringTemperatureDevice().getName() : null);

        map.put("monitoringHumidityDeviceId",
                entity.getMonitoringHumidityDevice() != null ? entity.getMonitoringHumidityDevice().getId() : null);
        map.put("monitoringHumidityDeviceName",
                entity.getMonitoringHumidityDevice() != null ? entity.getMonitoringHumidityDevice().getName() : null);

        map.put("monitoringLightDeviceId",
                entity.getMonitoringLightDevice() != null ? entity.getMonitoringLightDevice().getId() : null);
        map.put("monitoringLightDeviceName",
                entity.getMonitoringLightDevice() != null ? entity.getMonitoringLightDevice().getName() : null);

        map.put("monitoringMotionDeviceId",
                entity.getMonitoringMotionDevice() != null ? entity.getMonitoringMotionDevice().getId() : null);
        map.put("monitoringMotionDeviceName",
                entity.getMonitoringMotionDevice() != null ? entity.getMonitoringMotionDevice().getName() : null);

        map.put("createdById", entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null);
        map.put("createdByUsername", entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null);
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());

        return map;
    }

    public Map<String, Object> toDetail(String message, ConfigEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", message);

        if (entity != null) {
            map.put("configId", entity.getId());
            map.put("configName", entity.getName());
            map.put("isActive", entity.getIsActive());
        }

        return map;
    }
}