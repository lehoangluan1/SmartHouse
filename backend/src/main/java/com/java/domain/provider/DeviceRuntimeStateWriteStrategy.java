package com.java.domain.provider;

import com.java.domain.service.dto.DeviceRuntimeStateChange;
import com.java.domain.service.dto.DeviceRuntimeStateWriteContext;

public interface DeviceRuntimeStateWriteStrategy {
    boolean supports(String capabilityCode);
    DeviceRuntimeStateChange write(DeviceRuntimeStateWriteContext context);
}