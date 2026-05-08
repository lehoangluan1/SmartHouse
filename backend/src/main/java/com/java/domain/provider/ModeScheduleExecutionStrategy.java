package com.java.domain.provider;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.java.domain.service.DeviceRuntimeStateService;
import com.java.domain.service.HomeModeControlService;
import com.java.domain.service.ManualHoldQueryService;
import com.java.domain.service.ModeAutomationService;
import com.java.domain.service.ModeScheduleSupport;
import com.java.domain.service.dto.ScheduleExecutionContext;
import com.java.persistence.entity.ScheduleEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModeScheduleExecutionStrategy implements ScheduleExecutionStrategy {

    private static final String MODE_CAPABILITY = "MODE";

    private final ManualHoldQueryService manualHoldQueryService;
    private final ModeAutomationService modeAutomationService;
    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final HomeModeControlService homeModeControlService;
    private final ModeScheduleSupport modeScheduleSupport;

    @Override
    public boolean supports(ScheduleEntity schedule) {
        return schedule != null
                && modeScheduleSupport.isModeSchedule(schedule);
    }

    @Override
    @Transactional
    public void execute(ScheduleExecutionContext context) {
        ScheduleEntity schedule = context.schedule();

        if (!hasExecutableTarget(schedule)) {
            return;
        }

        Long deviceId = schedule.getDevice().getId();
        Long homeId = schedule.getDevice().getHome().getId();

        if (manualHoldQueryService.isHolding(deviceId)) {
            return;
        }

        String nextMode = modeScheduleSupport.normalizeModeValue(schedule.getValueText());
        if (nextMode == null) {
            return;
        }

        boolean changed = deviceRuntimeStateService.hasChanged(deviceId, MODE_CAPABILITY, nextMode);
        if (!changed) {
            return;
        }

        homeModeControlService.changeMode(
                homeId,
                nextMode,
                "scheduler",
                schedule.getId(),
                null,
                "scheduler"
        );

        modeAutomationService.evaluateAllByHome(homeId);
    }

    private boolean hasExecutableTarget(ScheduleEntity schedule) {
        return schedule != null
                && schedule.getDevice() != null
                && schedule.getDevice().getId() != null
                && schedule.getDevice().getHome() != null
                && schedule.getDevice().getHome().getId() != null
                && schedule.getValueText() != null
                && !schedule.getValueText().isBlank();
    }
}
