package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.persistence.entity.DeviceCapabilityEntity;
import com.java.persistence.entity.DeviceStateHistoryEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.DeviceCapabilityRepository;
import com.java.persistence.repo.DeviceStateHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceStateHistoryService {

    private final DeviceCapabilityRepository deviceCapabilityRepository;
    private final DeviceStateHistoryRepository deviceStateHistoryRepository;
    private final DeviceTargetPolicy deviceTargetPolicy;

    @Transactional
    public void recordBoolean(
            Long deviceId,
            String capabilityCode,
            Boolean value,
            String source,
            Long sourceRefId,
            UserEntity changedBy
    ) {
        if (deviceId == null || capabilityCode == null || value == null) {
            return;
        }

        DeviceCapabilityEntity capability = requireCapability(deviceId, capabilityCode);

        deviceStateHistoryRepository.save(DeviceStateHistoryEntity.builder()
                .capability(capability)
                .valueBoolean(value)
                .source(source)
                .sourceRefId(sourceRefId)
                .changedBy(changedBy)
                .createdAt(OffsetDateTime.now())
                .build());
    }

    @Transactional
    public void recordNumber(
            Long deviceId,
            String capabilityCode,
            Double value,
            String source,
            Long sourceRefId,
            UserEntity changedBy
    ) {
        if (deviceId == null || capabilityCode == null || value == null) {
            return;
        }

        DeviceCapabilityEntity capability = requireCapability(deviceId, capabilityCode);

        deviceStateHistoryRepository.save(DeviceStateHistoryEntity.builder()
                .capability(capability)
                .valueNumber(value)
                .source(source)
                .sourceRefId(sourceRefId)
                .changedBy(changedBy)
                .createdAt(OffsetDateTime.now())
                .build());
    }

    @Transactional
    public void recordText(
            Long deviceId,
            String capabilityCode,
            String value,
            String source,
            Long sourceRefId,
            UserEntity changedBy
    ) {
        if (deviceId == null || capabilityCode == null || value == null) {
            return;
        }

        DeviceCapabilityEntity capability = requireCapability(deviceId, capabilityCode);

        deviceStateHistoryRepository.save(DeviceStateHistoryEntity.builder()
                .capability(capability)
                .valueText(value)
                .source(source)
                .sourceRefId(sourceRefId)
                .changedBy(changedBy)
                .createdAt(OffsetDateTime.now())
                .build());
    }

    @Transactional
    public void recordValue(
            Long deviceId,
            String capabilityCode,
            Object value,
            String source,
            Long sourceRefId,
            UserEntity changedBy
    ) {
        if (value == null) {
            return;
        }

        if (value instanceof Boolean b) {
            recordBoolean(deviceId, capabilityCode, b, source, sourceRefId, changedBy);
            return;
        }

        if (value instanceof Number n) {
            recordNumber(deviceId, capabilityCode, n.doubleValue(), source, sourceRefId, changedBy);
            return;
        }

        recordText(deviceId, capabilityCode, String.valueOf(value).trim(), source, sourceRefId, changedBy);
    }

    private DeviceCapabilityEntity requireCapability(Long deviceId, String capabilityCode) {
        String normalizedCapabilityCode = deviceTargetPolicy.normalizeTarget(capabilityCode);

        DeviceCapabilityEntity capability =
                deviceCapabilityRepository.findByDeviceIdAndCapabilityCode(deviceId, normalizedCapabilityCode);

        if (capability == null) {
            throw new IllegalArgumentException(
                    "Capability " + normalizedCapabilityCode + " of device " + deviceId + " does not exist"
            );
        }

        return capability;
    }

    public String stringify(DeviceStateHistoryEntity entity) {
        if (entity == null) return "-";
        if (entity.getValueBoolean() != null) return String.valueOf(entity.getValueBoolean());
        if (entity.getValueNumber() != null) return String.valueOf(entity.getValueNumber());
        if (entity.getValueText() != null) return entity.getValueText();
        return "-";
    }
}