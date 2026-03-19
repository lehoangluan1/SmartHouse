package com.java.domain.provider;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.java.controller.dto.ModeScheduleUpsertRequest;
import com.java.domain.service.ModeScheduleSupport;
import com.java.domain.service.ModeValueResolver;
import com.java.domain.service.ScheduleTimeParser;
import com.java.domain.service.dto.ModeScheduleDefaults;
import com.java.domain.service.dto.ScheduleDefaults;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.ScheduleEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModeScheduleEntityFactory {

    private final ModeValueResolver modeValueResolver;
    private final ScheduleTimeParser scheduleTimeParser;
    
    public ScheduleEntity create(DeviceEntity device, ModeScheduleUpsertRequest request) {
        ScheduleEntity schedule = new ScheduleEntity();
        schedule.setDevice(device);
        schedule.setCapabilityCode(ModeScheduleSupport.MODE_CAPABILITY);
        schedule.setValueText(modeValueResolver.normalizeMode(request.mode()));
        schedule.setValueBoolean(null);
        schedule.setValueNumber(null);
        schedule.setStartTime(scheduleTimeParser.parseRequired(request.startTime(), "startTime"));
        schedule.setEndTime(scheduleTimeParser.parseNullable(request.endTime(), "endTime"));
        schedule.setDaysMask(
            Objects.requireNonNullElse(
                request.daysMask(),
                ScheduleDefaults.DEFAULT_DAYS_MASK
        ));
        schedule.setEnabled(
            Objects.requireNonNullElse(
                request.enabled(),
                ScheduleDefaults.DEFAULT_ENABLED
        ));
        schedule.setPriority(ModeScheduleDefaults.DEFAULT_PRIORITY);
        return schedule;
    }
}