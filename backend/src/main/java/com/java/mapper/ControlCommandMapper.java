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
                entity.getTarget(),
                capabilityValueSupport.commandValueAsString(entity)
        );
    }
}