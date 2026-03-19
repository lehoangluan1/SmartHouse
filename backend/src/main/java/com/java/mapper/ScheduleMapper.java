package com.java.mapper;

import com.java.controller.dto.ScheduleResponse;
import com.java.controller.dto.ScheduleUpsertRequest;
import com.java.domain.provider.ScheduleEntityWriter;
import com.java.domain.provider.ScheduleViewAssembler;
import com.java.domain.service.ScheduleTypedValue;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.ScheduleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleMapper {

    private final ScheduleEntityWriter entityWriter;
    private final ScheduleViewAssembler viewAssembler;

    public void apply(
            ScheduleEntity entity,
            DeviceEntity device,
            String capabilityCode,
            ScheduleTypedValue typedValue,
            ScheduleUpsertRequest request
    ) {
        entityWriter.apply(entity, device, capabilityCode, typedValue, request);
    }

    public ScheduleResponse toResponse(ScheduleEntity entity) {
        return viewAssembler.toResponse(entity);
    }
}