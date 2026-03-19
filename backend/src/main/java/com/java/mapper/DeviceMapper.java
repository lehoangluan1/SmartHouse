package com.java.mapper;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.java.controller.dto.DeviceCreateRequest;
import com.java.controller.dto.DeviceResponse;
import com.java.controller.dto.DeviceStateResponse;
import com.java.domain.provider.DeviceEntityFactory;
import com.java.domain.provider.DeviceRuntimeStateProvider;
import com.java.domain.provider.DeviceRuntimeViewAssembler;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeviceMapper {

    private final DeviceRuntimeStateProvider runtimeStateProvider;
    private final DeviceRuntimeViewAssembler runtimeViewAssembler;
    private final DeviceEntityFactory deviceEntityFactory;

    public DeviceResponse toDeviceResponse(DeviceEntity device) {
        return runtimeViewAssembler.assembleDeviceResponse(device, getStateMap(device));
    }

    public DeviceStateResponse toDeviceStateResponse(DeviceEntity device) {
        return runtimeViewAssembler.assembleDeviceStateResponse(device, getStateMap(device));
    }

    public DeviceEntity toNewEntity(DeviceCreateRequest request, HomeEntity home, UserEntity installedBy) {
        return deviceEntityFactory.create(request, home, installedBy);
    }

    private Map<String, DeviceRuntimeStateEntity> getStateMap(DeviceEntity device) {
        return runtimeStateProvider.getRuntimeStateMap(device.getId());
    }
}