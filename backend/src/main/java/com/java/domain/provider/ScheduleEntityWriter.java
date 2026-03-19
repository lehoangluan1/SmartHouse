package com.java.domain.provider;

import com.java.controller.dto.ScheduleUpsertRequest;
import com.java.domain.service.ScheduleTypedValue;
import com.java.domain.service.dto.ScheduleDefaults;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.ScheduleEntity;

import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class ScheduleEntityWriter {

    public void apply(
            ScheduleEntity entity,
            DeviceEntity device,
            String capabilityCode,
            ScheduleTypedValue typedValue,
            ScheduleUpsertRequest request
    ) {
        entity.setDevice(device);
        entity.setCapabilityCode(capabilityCode);

        entity.setValueBoolean(typedValue.boolValue());
        entity.setValueNumber(typedValue.numberValue());
        entity.setValueText(typedValue.textValue());

        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setDaysMask(
            Objects.requireNonNullElse(
                request.daysMask(),
                ScheduleDefaults.DEFAULT_DAYS_MASK
        ));
        entity.setEnabled(
            Objects.requireNonNullElse(
                request.enabled(),
                ScheduleDefaults.DEFAULT_ENABLED
        ));
        entity.setPriority(
            Objects.requireNonNullElse(
                request.priority(),
                ScheduleDefaults.DEFAULT_PRIORITY
        ));
    }
}