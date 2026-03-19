package com.java.domain.service;

import com.java.mapper.ConfigAuditMapper;
import com.java.persistence.entity.ConfigEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ConfigActivityLogger {

    private final ActivityLogService activityLogService;
    private final ConfigAuditMapper configAuditMapper;
    private final ObjectMapper objectMapper;

    public void logCreated(Long homeId, Long actorId, ConfigEntity created) {
        activityLogService.log(
                homeId,
                null,
                actorId,
                "CREATE_CONFIG",
                "api",
                null,
                toJson(configAuditMapper.toSnapshot(created)),
                toJson(configAuditMapper.toDetail("Config created", created))
        );
    }

    public void logUpdated(Long homeId, Long actorId, ConfigEntity before, ConfigEntity after) {
        activityLogService.log(
                homeId,
                null,
                actorId,
                "UPDATE_CONFIG",
                "api",
                toJson(configAuditMapper.toSnapshot(before)),
                toJson(configAuditMapper.toSnapshot(after)),
                toJson(configAuditMapper.toDetail("Config updated", after))
        );
    }

    public void logActivated(Long homeId, Long actorId, ConfigEntity previousActive, ConfigEntity activated) {
        Map<String, Object> detail = new LinkedHashMap<>(
                configAuditMapper.toDetail("Config activated", activated)
        );

        if (previousActive != null) {
            detail.put("previousActiveConfigId", previousActive.getId());
            detail.put("previousActiveConfigName", previousActive.getName());
        }

        activityLogService.log(
                homeId,
                null,
                actorId,
                "ACTIVATE_CONFIG",
                "api",
                toJson(configAuditMapper.toSnapshot(previousActive)),
                toJson(configAuditMapper.toSnapshot(activated)),
                toJson(detail)
        );
    }

    public void logDeleted(Long homeId, Long actorId, ConfigEntity deleted, boolean wasActive) {
        Map<String, Object> detail = new LinkedHashMap<>(
                configAuditMapper.toDetail(
                        wasActive ? "Deleted active config" : "Deleted config",
                        deleted
                )
        );
        detail.put("wasActive", wasActive);

        activityLogService.log(
                homeId,
                null,
                actorId,
                "DELETE_CONFIG",
                "api",
                toJson(configAuditMapper.toSnapshot(deleted)),
                null,
                toJson(detail)
        );
    }

    public void logAutoActivatedAfterDelete(Long homeId, Long actorId, ConfigEntity activated) {
        activityLogService.log(
                homeId,
                null,
                actorId,
                "AUTO_ACTIVATE_CONFIG",
                "system",
                null,
                toJson(configAuditMapper.toSnapshot(activated)),
                toJson(configAuditMapper.toDetail(
                        "Auto activated fallback config after deletion",
                        activated
                ))
        );
    }

    private String toJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            return String.valueOf(value);
        }
    }
}