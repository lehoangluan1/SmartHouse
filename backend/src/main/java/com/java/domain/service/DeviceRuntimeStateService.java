package com.java.domain.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.config.NotFoundException;
import com.java.domain.provider.DeviceRuntimeStateFactory;
import com.java.domain.provider.DeviceRuntimeStateValueReader;
import com.java.domain.provider.DeviceRuntimeStateWriteStrategyResolver;
import com.java.domain.service.dto.DeviceRuntimeStateChange;
import com.java.domain.service.dto.DeviceRuntimeStateWriteContext;
import com.java.eventing.DomainEventBus;
import com.java.eventing.HomeModeChangedEvent;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.entity.DeviceRuntimeStateId;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.CapabilityRepository;
import com.java.persistence.repo.DeviceRepository;
import com.java.persistence.repo.DeviceRuntimeStateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceRuntimeStateService {

    private static final String MODE_CAPABILITY = "MODE";

    private final DeviceRepository deviceRepository;
    private final DeviceRuntimeStateRepository runtimeStateRepository;
    private final CapabilityRepository capabilityRepository;
    private final DeviceTargetPolicy deviceTargetPolicy;
    private final DeviceRuntimeStateFactory runtimeStateFactory;
    private final DeviceRuntimeStateValueReader valueReader;
    private final RuntimeStateValueNormalizer valueNormalizer;
    private final DeviceRuntimeStateWriteStrategyResolver writeStrategyResolver;
    private final DeviceRuntimeStateRealtimePublisher realtimePublisher;
    private final DomainEventBus eventBus;

    @Transactional(readOnly = true)
    public Map<String, DeviceRuntimeStateEntity> getStateMap(Long deviceId) {
        return runtimeStateRepository.findByIdDeviceId(deviceId).stream()
                .collect(Collectors.toMap(
                        DeviceRuntimeStateEntity::getCapabilityCode,
                        Function.identity(),
                        (a, b) -> b
                ));
    }

    @Transactional(readOnly = true)
    public DeviceRuntimeStateEntity getState(Long deviceId, String capabilityCode) {
        String normalizedCapabilityCode = normalizeCapability(capabilityCode);

        return runtimeStateRepository.findById(
                new DeviceRuntimeStateId(deviceId, normalizedCapabilityCode)
        ).orElse(null);
    }

    @Transactional
    public DeviceRuntimeStateEntity upsertValue(Long deviceId, String capabilityCode, Object value) {
        return applyState(deviceId, capabilityCode, value, null, null, null).entity();
    }

    @Transactional
    public StateWriteResult upsertValueAndRecordHistory(
            Long deviceId,
            String capabilityCode,
            Object value,
            String source,
            Long sourceRefId,
            UserEntity changedBy
    ) {
        return applyState(deviceId, capabilityCode, value, source, sourceRefId, changedBy);
    }

    @Transactional
    public StateWriteResult applyState(
            Long deviceId,
            String capabilityCode,
            Object value,
            String source,
            Long sourceRefId,
            UserEntity changedBy
    ) {
        String normalizedCapabilityCode = normalizeCapability(capabilityCode);
        DeviceEntity device = getRequiredDevice(deviceId);

        validateCapabilityExists(device.getId(), normalizedCapabilityCode);

        DeviceRuntimeStateEntity currentState = getOrCreateState(device, normalizedCapabilityCode);

        var strategy = writeStrategyResolver.resolve(normalizedCapabilityCode);
        if (strategy == null) {
            throw new BadRequestException(
                    "Unsupported runtime state capability: " + normalizedCapabilityCode
            );
        }

        DeviceRuntimeStateWriteContext context = new DeviceRuntimeStateWriteContext(
                device,
                currentState,
                normalizedCapabilityCode,
                value,
                source,
                sourceRefId,
                changedBy
        );

        DeviceRuntimeStateChange change = strategy.write(context);

        StateWriteResult result = new StateWriteResult(
                change.entity(),
                change.previousValue(),
                change.nextValue(),
                change.changed(),
                change.historyRecorded()
        );

        realtimePublisher.publishIfNeeded(device, normalizedCapabilityCode, result);

        return result;
    }

    @Transactional(readOnly = true)
    public boolean hasChanged(Long deviceId, String capabilityCode, Object nextValue) {
        String normalizedCapabilityCode = normalizeCapability(capabilityCode);

        if (!capabilityRepository.existsByDevice_IdAndId_CapabilityCode(deviceId, normalizedCapabilityCode)) {
            return false;
        }

        DeviceRuntimeStateEntity current = getState(deviceId, normalizedCapabilityCode);

        Object currentValue = valueReader.read(current);
        Object normalizedNextValue = valueNormalizer.normalizeInput(nextValue);

        return !Objects.equals(
                valueNormalizer.normalizeComparable(currentValue),
                valueNormalizer.normalizeComparable(normalizedNextValue)
        );
    }

    @Transactional
    public void syncModeForHome(Long homeId, String mode) {
        syncModeForHome(homeId, mode, null, null, null);
    }

    @Transactional
    public void syncModeForHome(
            Long homeId,
            String mode,
            String source,
            Long sourceRefId,
            UserEntity changedBy
    ) {
        List<DeviceEntity> devices = deviceRepository.findByHomeId(homeId);
        String normalizedModeCapability = normalizeCapability(MODE_CAPABILITY);

        Long eventDeviceId = null;
        boolean changed = false;

        for (DeviceEntity device : devices) {
            if (!supportsModeSync(device, normalizedModeCapability)) {
                continue;
            }

            StateWriteResult result = applyState(
                    device.getId(),
                    normalizedModeCapability,
                    mode,
                    source,
                    sourceRefId,
                    changedBy
            );

            if (result.changed()) {
                changed = true;
                if (eventDeviceId == null) {
                    eventDeviceId = device.getId();
                }
            }
        }

        if (changed) {
            eventBus.publish(HomeModeChangedEvent.builder()
                    .homeId(homeId)
                    .deviceId(eventDeviceId)
                    .mode(normalizeModeValue(mode))
                    .build());
        }
    }

    private boolean supportsModeSync(DeviceEntity device, String capabilityCode) {
        return deviceTargetPolicy.supportsModeSync(device)
                && capabilityRepository.existsByDevice_IdAndId_CapabilityCode(device.getId(), capabilityCode);
    }

    private String normalizeModeValue(String mode) {
        return mode == null ? null : mode.trim().toUpperCase();
    }

    private String normalizeCapability(String capabilityCode) {
        String normalized = deviceTargetPolicy.normalizeTarget(capabilityCode);

        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("Capability code must not be blank");
        }

        return normalized.trim().toUpperCase();
    }

    private DeviceEntity getRequiredDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device does not exist: " + deviceId));
    }

    private void validateCapabilityExists(Long deviceId, String capabilityCode) {
        boolean exists = capabilityRepository.existsByDevice_IdAndId_CapabilityCode(deviceId, capabilityCode);

        if (!exists) {
            throw new BadRequestException(
                    "Capability " + capabilityCode + " is not configured for device " + deviceId
            );
        }
    }

    private DeviceRuntimeStateEntity getOrCreateState(DeviceEntity device, String capabilityCode) {
        DeviceRuntimeStateId id = new DeviceRuntimeStateId(device.getId(), capabilityCode);

        return runtimeStateRepository.findById(id)
                .orElseGet(() -> runtimeStateFactory.create(device, capabilityCode));
    }

    public record StateWriteResult(
            DeviceRuntimeStateEntity entity,
            Object previousValue,
            Object nextValue,
            boolean changed,
            boolean historyRecorded
    ) {
    }
}