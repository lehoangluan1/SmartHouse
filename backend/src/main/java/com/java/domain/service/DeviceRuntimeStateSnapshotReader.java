package com.java.domain.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.repo.DeviceRuntimeStateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeviceRuntimeStateSnapshotReader {

    private final DeviceRuntimeStateRepository runtimeStateRepository;

    public Map<Long, Map<String, Object>> readByDevices(List<DeviceEntity> devices) {
        List<Long> deviceIds = devices.stream()
                .map(DeviceEntity::getId)
                .toList();

        if (deviceIds.isEmpty()) {
            return Map.of();
        }

        return runtimeStateRepository.findByIdDeviceIdIn(deviceIds)
                .stream()
                .collect(Collectors.groupingBy(
                        state -> state.getDevice().getId(),
                        Collectors.toMap(
                                DeviceRuntimeStateEntity::getCapabilityCode,
                                this::extractValue,
                                (a, b) -> b
                        )
                ));
    }

    private Object extractValue(DeviceRuntimeStateEntity entity) {
        if (entity.getValueBoolean() != null) {
            return entity.getValueBoolean();
        }
        if (entity.getValueNumber() != null) {
            return entity.getValueNumber();
        }
        return entity.getValueText();
    }
}