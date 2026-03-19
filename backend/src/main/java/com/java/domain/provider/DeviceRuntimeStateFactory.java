package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;

@Component
public class DeviceRuntimeStateFactory {

    public DeviceRuntimeStateEntity create(DeviceEntity device, String capabilityCode) {
        DeviceRuntimeStateEntity entity = new DeviceRuntimeStateEntity();
        entity.setDevice(device);
        entity.setCapabilityCode(capabilityCode);
        return entity;
    }
}