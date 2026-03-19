package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.DeviceRuntimeStateEntity;

@Component
public class DefaultDeviceRuntimeStateValueReader implements DeviceRuntimeStateValueReader {

    @Override
    public Object read(DeviceRuntimeStateEntity entity) {
        if (entity == null) {
            return null;
        }

        if (entity.getValueNumber() != null) {
            return entity.getValueNumber();
        }

        if (entity.getValueBoolean() != null) {
            return entity.getValueBoolean();
        }

        if (entity.getValueText() != null) {
            return entity.getValueText();
        }

        return null;
    }
}