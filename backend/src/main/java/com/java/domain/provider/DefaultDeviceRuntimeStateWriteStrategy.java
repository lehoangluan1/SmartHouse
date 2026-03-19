package com.java.domain.provider;

import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.java.domain.service.CapabilityValueSupport;
import com.java.domain.service.DeviceStateHistoryService;
import com.java.domain.service.RuntimeStateHistoryPolicy;
import com.java.domain.service.RuntimeStateValueNormalizer;
import com.java.domain.service.dto.DeviceRuntimeStateChange;
import com.java.domain.service.dto.DeviceRuntimeStateWriteContext;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.repo.DeviceRuntimeStateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultDeviceRuntimeStateWriteStrategy implements DeviceRuntimeStateWriteStrategy {

    private final DeviceRuntimeStateRepository runtimeStateRepository;
    private final CapabilityValueSupport capabilityValueSupport;
    private final DeviceRuntimeStateValueReader valueReader;
    private final RuntimeStateValueNormalizer valueNormalizer;
    private final RuntimeStateHistoryPolicy historyPolicy;
    private final DeviceStateHistoryService deviceStateHistoryService;

    @Override
    public boolean supports(String capabilityCode) {
        return true;
    }

    @Override
    public DeviceRuntimeStateChange write(DeviceRuntimeStateWriteContext context) {
        DeviceRuntimeStateEntity entity = context.currentState();

        Object previousValue = valueReader.read(entity);
        Object nextValue = valueNormalizer.normalizeInput(context.requestedValue());

        boolean changed = !Objects.equals(
                valueNormalizer.normalizeComparable(previousValue),
                valueNormalizer.normalizeComparable(nextValue)
        );

        capabilityValueSupport.assignRuntimeValue(entity, nextValue);
        entity.setUpdatedAt(OffsetDateTime.now());

        DeviceRuntimeStateEntity saved = runtimeStateRepository.save(entity);

        boolean historyRecorded = false;
        if (historyPolicy.shouldRecord(context, previousValue, nextValue, changed)) {
            deviceStateHistoryService.recordValue(
                    context.device().getId(),
                    context.capabilityCode(),
                    nextValue,
                    context.source(),
                    context.sourceRefId(),
                    context.changedBy()
            );
            historyRecorded = true;
        }

        return new DeviceRuntimeStateChange(
                saved,
                previousValue,
                nextValue,
                changed,
                historyRecorded
        );
    }
}