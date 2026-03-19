package com.java.mapper;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.java.controller.dto.AuditEventItem;
import com.java.domain.AlertStatus;
import com.java.persistence.entity.AlertEntity;

@Component
public class AlertAuditEventMapper {

    public AuditEventItem map(AlertEntity entity) {
        String status = entity.getStatus() != null ? entity.getStatus().name() : "ACTIVE";

        return AuditEventItem.builder()
                .source("ALERT")
                .id(entity.getId())
                .category("alerts")
                .type(entity.getType() != null ? entity.getType().name() : "ALERT")
                .status(status)
                .homeId(entity.getHome() != null ? entity.getHome().getId() : null)
                .deviceId(entity.getDevice() != null ? entity.getDevice().getId() : null)
                .deviceName(entity.getDevice() != null ? entity.getDevice().getName() : null)
                .userId(
                        entity.getAcknowledgedBy() != null
                                ? entity.getAcknowledgedBy().getId()
                                : entity.getResolvedBy() != null
                                        ? entity.getResolvedBy().getId()
                                        : null
                )
                .username(
                        entity.getAcknowledgedBy() != null
                                ? entity.getAcknowledgedBy().getUsername()
                                : entity.getResolvedBy() != null
                                        ? entity.getResolvedBy().getUsername()
                                        : "System"
                )
                .method("ALERT")
                .fromState(resolveFromState(entity.getStatus()))
                .toState(resolveToState(entity.getStatus()))
                .details(entity.getMessage() != null && !entity.getMessage().isBlank()
                        ? entity.getMessage()
                        : entity.getType() != null ? entity.getType().name() : "-")
                .createdAt(resolveCreatedAt(entity))
                .build();
    }

    private String resolveFromState(AlertStatus status) {
        if (status == null) {
            return "-";
        }

        return switch (status) {
            case ACTIVE -> "-";
            case ACK, RESOLVED -> "ACTIVE";
        };
    }

    private String resolveToState(AlertStatus status) {
        if (status == null) {
            return "ACTIVE";
        }
        return status.name();
    }

    private OffsetDateTime resolveCreatedAt(AlertEntity entity) {
        if (entity.getStatus() == AlertStatus.RESOLVED && entity.getResolvedAt() != null) {
            return entity.getResolvedAt();
        }
        if (entity.getStatus() == AlertStatus.ACK && entity.getAcknowledgedAt() != null) {
            return entity.getAcknowledgedAt();
        }
        if (entity.getLastTriggeredAt() != null) {
            return entity.getLastTriggeredAt();
        }
        return entity.getCreatedAt();
    }
}