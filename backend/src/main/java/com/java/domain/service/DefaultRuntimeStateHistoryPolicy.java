package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.domain.service.dto.DeviceRuntimeStateWriteContext;

@Component
public class DefaultRuntimeStateHistoryPolicy implements RuntimeStateHistoryPolicy {

    @Override
    public boolean shouldRecord(
            DeviceRuntimeStateWriteContext context,
            Object previousValue,
            Object nextValue,
            boolean changed
    ) {
        return changed
                && context.source() != null
                && !context.source().isBlank()
                && nextValue != null;
    }
}