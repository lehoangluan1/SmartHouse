package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.controller.dto.ModeScheduleUpsertRequest;
import com.java.domain.service.ModeValueResolver;
import com.java.domain.service.ScheduleTimeParser;
import com.java.persistence.entity.ScheduleEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModeScheduleUpdater {

    private final ModeValueResolver modeValueResolver;
    private final ScheduleTimeParser scheduleTimeParser;

    public void merge(ScheduleEntity schedule, ModeScheduleUpsertRequest request) {
        if (request.mode() != null && !request.mode().isBlank()) {
            schedule.setValueText(modeValueResolver.normalizeMode(request.mode()));
        }

        if (request.startTime() != null && !request.startTime().isBlank()) {
            schedule.setStartTime(scheduleTimeParser.parseRequired(request.startTime(), "startTime"));
        }

        if (request.endTime() != null) {
            schedule.setEndTime(scheduleTimeParser.parseNullable(request.endTime(), "endTime"));
        }

        if (request.daysMask() != null) {
            schedule.setDaysMask(request.daysMask());
        }

        if (request.enabled() != null) {
            schedule.setEnabled(request.enabled());
        }
    }
}