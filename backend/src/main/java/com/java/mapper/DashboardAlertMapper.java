package com.java.mapper;

import org.springframework.stereotype.Component;

import com.java.controller.dto.AlertResponse;
import com.java.persistence.entity.AlertEntity;

@Component
public class DashboardAlertMapper {

    public AlertResponse toResponse(AlertEntity alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .homeId(alert.getHome() != null ? alert.getHome().getId() : null)
                .deviceId(alert.getDevice() != null ? alert.getDevice().getId() : null)
                .sensorId(alert.getSensor() != null ? alert.getSensor().getId() : null)
                .type(alert.getType() != null ? alert.getType().name() : null)
                .message(alert.getMessage())
                .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                .acknowledgedBy(alert.getAcknowledgedBy() != null ? alert.getAcknowledgedBy().getId() : null)
                .acknowledgedAt(alert.getAcknowledgedAt())
                .resolvedBy(alert.getResolvedBy() != null ? alert.getResolvedBy().getId() : null)
                .resolvedAt(alert.getResolvedAt())
                .createdAt(alert.getCreatedAt())
                .lastTriggeredAt(alert.getLastTriggeredAt())
                .build();
    }
}