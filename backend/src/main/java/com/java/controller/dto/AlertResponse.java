package com.java.controller.dto;

import java.time.OffsetDateTime;

import com.java.persistence.entity.AlertEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {
    private Long id;
    private Long homeId;
    private Long deviceId;
    private Long sensorId;
    private String type;
    private String message;
    private String status;
    private Long acknowledgedBy;
    private OffsetDateTime acknowledgedAt;
    private Long resolvedBy;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastTriggeredAt;

    public static AlertResponse from(AlertEntity e) {
        return AlertResponse.builder()
                .id(e.getId())
                .homeId(e.getHome() != null ? e.getHome().getId() : null)
                .deviceId(e.getDevice() != null ? e.getDevice().getId() : null)
                .sensorId(e.getSensor() != null ? e.getSensor().getId() : null)
                .type(e.getType() != null ? e.getType().name() : null)
                .message(e.getMessage())
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .acknowledgedBy(e.getAcknowledgedBy() != null ? e.getAcknowledgedBy().getId() : null)
                .acknowledgedAt(e.getAcknowledgedAt())
                .resolvedBy(e.getResolvedBy() != null ? e.getResolvedBy().getId() : null)
                .resolvedAt(e.getResolvedAt())
                .createdAt(e.getCreatedAt())
                .lastTriggeredAt(e.getLastTriggeredAt())
                .build();
    }
}