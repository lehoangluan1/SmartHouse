package com.java.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.java.controller.dto.ControlCommandResponse;
import com.java.controller.dto.ControlRequest;
import com.java.mapper.ControlCommandMapper;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.ControlCommandRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeviceCommandExecutionService {

    private static final String SOURCE_MANUAL_CONTROL = "MANUAL_CONTROL";

    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final UserRepository userRepository;
    private final ControlCommandFactory controlCommandFactory;
    private final ControlCommandRepository controlCommandRepository;
    private final ControlCommandSender controlCommandSender;
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

        var command = controlCommandFactory.createManual(
                device,
                normalized.target(),
                normalized.value(),
                actor,
                request.actorName()
        );

        command = controlCommandRepository.save(command);

        DeviceRuntimeStateService.StateWriteResult stateWriteResult =
                deviceRuntimeStateService.upsertValueAndRecordHistory(
                        device.getId(),
                        normalized.target(),
                        normalized.value(),
                        SOURCE_MANUAL_CONTROL,
                        command.getId(),
                        actor
                );

        command = controlCommandSender.sendNow(command);

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