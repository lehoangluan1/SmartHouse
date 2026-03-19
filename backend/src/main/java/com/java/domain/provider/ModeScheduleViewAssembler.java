package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.controller.dto.ModeScheduleResponse;
import com.java.persistence.entity.ScheduleEntity;

@Component
public class ModeScheduleViewAssembler {

    public ModeScheduleResponse toResponse(ScheduleEntity schedule) {
        return ModeScheduleResponse.builder()
                .id(schedule.getId())
                .homeId(schedule.getDevice() != null && schedule.getDevice().getHome() != null
                        ? schedule.getDevice().getHome().getId()
                        : null)
                .deviceId(schedule.getDevice() != null ? schedule.getDevice().getId() : null)
                .mode(schedule.getValueText())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .daysMask(schedule.getDaysMask())
                .enabled(schedule.getEnabled())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}