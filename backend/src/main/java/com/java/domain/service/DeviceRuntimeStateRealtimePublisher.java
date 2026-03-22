package com.java.domain.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.java.eventing.DeviceStateChangedEvent;
import com.java.eventing.DomainEventBus;
import com.java.persistence.entity.DeviceEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeviceRuntimeStateRealtimePublisher {

    private static final String POWER_CAPABILITY = "POWER";
    private static final String SPEED_CAPABILITY = "SPEED";
    private static final String BRIGHTNESS_CAPABILITY = "BRIGHTNESS";

    private static final Set<String> REALTIME_CAPABILITIES = Set.of(
            POWER_CAPABILITY,
            SPEED_CAPABILITY,
            BRIGHTNESS_CAPABILITY
    );

    private final DeviceRuntimeStateSnapshotReader snapshotReader;
    private final DomainEventBus eventBus;

    public void publishIfNeeded(
            DeviceEntity device,
            String capabilityCode,
            DeviceRuntimeStateService.StateWriteResult result
    ) {
        if (!isPublishable(device, capabilityCode, result)) {
            return;
        }

        Map<String, Object> snapshot = snapshotReader.readByDevices(List.of(device))
                .getOrDefault(device.getId(), Map.of());

        String status = asOnOff(snapshot.get(POWER_CAPABILITY));
        Integer speed = asInteger(snapshot.get(SPEED_CAPABILITY));
        Integer brightness = asInteger(snapshot.get(BRIGHTNESS_CAPABILITY));

        eventBus.publish(DeviceStateChangedEvent.builder()
                .homeId(device.getHome().getId())
                .deviceId(device.getId())
                .status(status)
                .speed(speed)
                .brightness(brightness)
                .build());
    }

    private boolean isPublishable(
            DeviceEntity device,
            String capabilityCode,
            DeviceRuntimeStateService.StateWriteResult result
    ) {
        if (device == null || device.getId() == null || device.getHome() == null || device.getHome().getId() == null) {
            return false;
        }

        if (result == null || !result.changed()) {
            return false;
        }

        String normalizedCapability = capabilityCode == null ? "" : capabilityCode.trim().toUpperCase();
        return REALTIME_CAPABILITIES.contains(normalizedCapability);
    }

    private String asOnOff(Object value) {
        if (!(value instanceof Boolean bool)) {
            return null;
        }
        return bool ? "ON" : "OFF";
    }

    private Integer asInteger(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        return number.intValue();
    }
}