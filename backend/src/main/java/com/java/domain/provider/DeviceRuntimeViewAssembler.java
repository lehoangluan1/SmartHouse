package com.java.domain.provider;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import com.java.controller.dto.DeviceResponse;
import com.java.controller.dto.DeviceStateResponse;
import com.java.mapper.DeviceStateProjector;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class DeviceRuntimeViewAssembler {

    private final List<DeviceRuntimeProjectionStrategy> strategies;
    private final DeviceRuntimeMetadataResolver metadataResolver;

    public DeviceResponse assembleDeviceResponse(
            DeviceEntity device,
            Map<String, DeviceRuntimeStateEntity> stateMap
    ) {
        
        DeviceResponse.DeviceResponseBuilder builder = DeviceResponse.builder()
                .id(device.getId())
                .name(device.getName())
                .deviceKey(device.getDeviceKey())
                .type(metadataResolver.resolveDeviceType(device))
                .subtype(device.getSubtype())
                .status(device.getStatus() != null ? device.getStatus().name() : null)
                .homeId(device.getHome() != null ? device.getHome().getId() : null)
                .roomName(device.getRoomName())
                .online(Boolean.TRUE.equals(device.getIsOnline()))
                .lastSeen(device.getLastSeen())
                .mode(metadataResolver.resolveMode(stateMap))
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt());
        
        
        strategies.stream()
                .filter(strategy -> strategy.supports(device.getSubtype()))
                .findFirst()
                .ifPresent(strategy -> strategy.apply(stateMap, DeviceStateProjector.forDeviceResponse(builder)));

        return builder.build();
    }

    public DeviceStateResponse assembleDeviceStateResponse(
            DeviceEntity device,
            Map<String, DeviceRuntimeStateEntity> stateMap
    ) {

        DeviceStateResponse.DeviceStateResponseBuilder builder = DeviceStateResponse.builder()
                .deviceId(device.getId())
                .mode(metadataResolver.resolveMode(stateMap))
                .holdUntil(null)
                .prevMode(null)
                .updatedAt(metadataResolver.resolveLatestUpdatedAt(stateMap));

        strategies.stream()
                .filter(strategy -> strategy.supports(device.getSubtype()))
                .findFirst()
                .ifPresent(strategy -> strategy.apply(stateMap, DeviceStateProjector.forDeviceStateResponse(builder)));

        return builder.build();
    }
}