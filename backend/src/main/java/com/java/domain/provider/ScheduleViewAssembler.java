package com.java.domain.provider;

import com.java.controller.dto.ScheduleResponse;
import com.java.persistence.entity.ScheduleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleViewAssembler {

    private final ScheduleValueReader scheduleValueReader;

    public ScheduleResponse toResponse(ScheduleEntity entity) {
        return ScheduleResponse.builder()
                .id(entity.getId())
                .deviceId(entity.getDevice() != null ? entity.getDevice().getId() : null)
                .capabilityCode(entity.getCapabilityCode())
                .value(scheduleValueReader.readAsText(entity))
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .daysMask(entity.getDaysMask())
                .enabled(entity.getEnabled())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}