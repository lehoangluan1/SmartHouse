package com.java.domain.service;

import com.java.domain.service.dto.DeviceRuntimeStateWriteContext;

public interface RuntimeStateHistoryPolicy {
    boolean shouldRecord(DeviceRuntimeStateWriteContext context, Object previousValue, Object nextValue, boolean changed);
}