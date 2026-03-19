package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.ScheduleEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleActivityLogger {

    private final ActivityLogService activityLogService;
    private final ActivityLogPayloadBuilder activityLogPayloadBuilder;

    public void logUpsert(ScheduleEntity schedule) {
        activityLogService.log(
                schedule.getDevice() != null && schedule.getDevice().getHome() != null
                        ? schedule.getDevice().getHome().getId()
                        : null,
                schedule.getDevice() != null ? schedule.getDevice().getId() : null,
                null,
                "UPSERT_SCHEDULE",
                "api",
                null,
                null,
                activityLogPayloadBuilder.controlPayload(
                        schedule.getCapabilityCode(),
                        extractValue(schedule)
                )
        );
    }

    private String extractValue(ScheduleEntity entity) {
        if (entity.getValueBoolean() != null) {
            return String.valueOf(entity.getValueBoolean());
        }
        if (entity.getValueNumber() != null) {
            return String.valueOf(entity.getValueNumber());
        }
        return entity.getValueText();
    }
}