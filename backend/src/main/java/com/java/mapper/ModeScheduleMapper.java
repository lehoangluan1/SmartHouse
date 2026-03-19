package com.java.mapper;

import com.java.controller.dto.ModeScheduleResponse;
import com.java.controller.dto.ModeScheduleUpsertRequest;
import com.java.domain.provider.ModeScheduleEntityFactory;
import com.java.domain.provider.ModeScheduleUpdater;
import com.java.domain.provider.ModeScheduleViewAssembler;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.ScheduleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModeScheduleMapper {

    private final ModeScheduleEntityFactory entityFactory;
    private final ModeScheduleUpdater updater;
    private final ModeScheduleViewAssembler viewAssembler;

    public ScheduleEntity toNewModeSchedule(DeviceEntity device, ModeScheduleUpsertRequest request) {
        return entityFactory.create(device, request);
    }

    public void merge(ScheduleEntity schedule, ModeScheduleUpsertRequest request) {
        updater.merge(schedule, request);
    }

    public ModeScheduleResponse toResponse(ScheduleEntity schedule) {
        return viewAssembler.toResponse(schedule);
    }
}