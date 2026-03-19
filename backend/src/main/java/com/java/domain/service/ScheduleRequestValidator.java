package com.java.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.controller.dto.ScheduleUpsertRequest;

@Component
public class ScheduleRequestValidator {

    private static final int DEFAULT_DAYS_MASK = 127;

    public void validate(ScheduleUpsertRequest request) {
        if (request == null) {
            throw new BadRequestException("Invalid request");
        }
        if (request.deviceId() == null) {
            throw new BadRequestException("deviceId must not be blank");
        }
        if (request.startTime() == null) {
            throw new BadRequestException("startTime must not be blank");
        }

        int daysMask = Optional.ofNullable(request).map(ScheduleUpsertRequest::daysMask).orElse( DEFAULT_DAYS_MASK);
        if (daysMask <= 0) {
            throw new BadRequestException("invalid daysMask");
        }

        boolean hasCapability = request.capabilityCode() != null && !request.capabilityCode().isBlank();
        boolean hasMode = request.mode() != null && !request.mode().isBlank();

        if (!hasCapability && !hasMode) {
            throw new BadRequestException("capabilityCode or mode must not be blank");
        }
    }
}
