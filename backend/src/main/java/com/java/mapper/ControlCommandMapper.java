package com.java.mapper;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.java.controller.dto.ControlCommandResponse;
import com.java.controller.dto.NextCommandResponse;
import com.java.domain.service.CapabilityValueSupport;
import com.java.persistence.entity.ControlCommandEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ControlCommandMapper {

    private final CapabilityValueSupport capabilityValueSupport;

    public ControlCommandResponse toResponse(ControlCommandEntity entity) {
        return ControlCommandResponse.builder()
                .id(entity.getId())
                .deviceId(entity.getDevice() != null ? entity.getDevice().getId() : null)
                .target(entity.getTarget())
                .value(capabilityValueSupport.commandValueAsString(entity))
                .actorId(entity.getActor() != null ? entity.getActor().getId() : null)
                .actorName(entity.getActorName())
                .source(resolveSource(entity))
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .ackAt(entity.getAckAt())
                .build();
    }

    public ControlCommandResponse toStatusResponse(
            Long deviceId,
            String target,
            Object value,
            Long actorId,
            String actorName,
            String status
    ) {
        return ControlCommandResponse.builder()
                .id(null)
                .deviceId(deviceId)
                .target(target)
                .value(value != null ? String.valueOf(value) : null)
                .actorId(actorId)
                .actorName(actorName)
                .source(null)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .sentAt(null)
                .ackAt(null)
                .build();
    }

    public NextCommandResponse toNextResponse(ControlCommandEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NextCommandResponse(
                entity.getId(),
                entity.getDevice() != null && entity.getDevice().getHome() != null
                        ? entity.getDevice().getHome().getId()
                        : null,
                entity.getDevice() != null ? entity.getDevice().getDeviceKey() : null,
                externalTarget(entity),
                externalValue(entity),
                resolveSource(entity)
        );
    }

    public String externalTarget(ControlCommandEntity entity) {
        String target = entity.getTarget();
        if (target == null) {
            return null;
        }

        return switch (target.trim().toUpperCase()) {
            case "POWER" -> "power";
            case "SPEED" -> "fan_speed";
            case "BRIGHTNESS" -> "brightness";
            case "MODE" -> "mode";
            default -> target.trim().toLowerCase();
        };
    }

    public String externalValue(ControlCommandEntity entity) {
        if (entity != null && entity.getValueBoolean() != null) {
            return entity.getValueBoolean() ? "on" : "off";
        }
        String value = capabilityValueSupport.commandValueAsString(entity);
        return value == null ? null : value.toLowerCase();
    }

    private String resolveSource(ControlCommandEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getSource() != null && !entity.getSource().isBlank()) {
            return entity.getSource().trim().toLowerCase();
        }
        if (entity.getActor() != null) {
            return "manual";
        }
        String actorName = entity.getActorName();
        if (actorName != null && "SYSTEM".equalsIgnoreCase(actorName.trim())) {
            return "system";
        }
        return actorName == null || actorName.isBlank() ? "system" : "manual";
    }
}
