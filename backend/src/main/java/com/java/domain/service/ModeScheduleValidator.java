package com.java.domain.service;

import com.java.config.BadRequestException;
import com.java.persistence.entity.ScheduleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModeScheduleValidator {

    private final ModeValueResolver modeValueResolver;
    private final ModeScheduleSupport modeScheduleSupport;

    public void validateBelongToHomeAndMode(Long homeId, ScheduleEntity schedule) {
        if (schedule.getDevice() == null || schedule.getDevice().getHome() == null || schedule.getDevice().getHome().getId() == null) {
            throw new BadRequestException("Schedule does not belong to a valid home");
        }

        if (!schedule.getDevice().getHome().getId().equals(homeId)) {
            throw new BadRequestException("Schedule does not belong to this home");
        }

        if (!modeScheduleSupport.isModeSchedule(schedule)) {
            throw new BadRequestException("This schedule is not a mode schedule");
        }
    }

    public void validateSchedule(ScheduleEntity schedule) {
        if (schedule.getDevice() == null || schedule.getDevice().getId() == null) {
            throw new BadRequestException("device is required");
        }

        if (schedule.getCapabilityCode() == null || schedule.getCapabilityCode().isBlank()) {
            throw new BadRequestException("capabilityCode is required");
        }

        if (schedule.getValueText() == null || schedule.getValueText().isBlank()) {
            throw new BadRequestException("mode is required");
        }

        modeValueResolver.validateNormalizedMode(schedule.getValueText());

        if (schedule.getStartTime() == null) {
            throw new BadRequestException("startTime is required");
        }

        if (schedule.getDaysMask() == null || schedule.getDaysMask() < 0 || schedule.getDaysMask() > 127) {
            throw new BadRequestException("daysMask must be in range 0..127");
        }
    }
}