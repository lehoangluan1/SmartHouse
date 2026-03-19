package com.java.domain.provider;

import com.java.persistence.entity.DeviceRuntimeStateEntity;

public interface DeviceRuntimeStateValueReader {
    Object read(DeviceRuntimeStateEntity entity);
}