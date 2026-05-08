package com.java.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.java.controller.dto.ControlCommandResponse;
import com.java.controller.dto.ControlRequest;
import com.java.mapper.ControlCommandMapper;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeviceCommandExecutionService {

    private static final String SOURCE_MANUAL_CONTROL = "MANUAL_CONTROL";

    private final UserRepository userRepository;
    private final DeviceControlService deviceControlService;
    private final ActivityLogService activityLogService;
    private final ActivityLogPayloadBuilder activityLogPayloadBuilder;
    private final ControlCommandMapper controlCommandMapper;

    @Transactional
    public ControlCommandResponse executeManual(
            DeviceEntity device,
            ControlRequest request,
            NormalizedControlRequest normalized
    ) {
        UserEntity actor = Optional.ofNullable(request.actorId())
                                .map(userRepository::getReferenceById)
                                .orElse(null);

        DeviceControlService.ControlResult controlResult = deviceControlService.controlDevice(
                device,
                normalized.target(),
                normalized.value(),
                "manual",
                actor,
                request.actorName()
        );

        var command = controlResult.command();
        DeviceRuntimeStateService.StateWriteResult stateWriteResult = controlResult.stateWriteResult();

        activityLogService.log(
                device.getHome() != null ? device.getHome().getId() : null,
                device.getId(),
                request.actorId(),
                SOURCE_MANUAL_CONTROL,
                request.method(),
                null,
                null,
                activityLogPayloadBuilder.controlPayload(
                        normalized.target(),
                        stateWriteResult.previousValue(),
                        stateWriteResult.nextValue()
                )
        );

        return controlCommandMapper.toResponse(command);
    }
}
