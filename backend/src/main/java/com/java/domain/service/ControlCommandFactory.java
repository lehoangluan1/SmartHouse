package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.domain.CommandStatus;
import com.java.persistence.entity.ControlCommandEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ControlCommandFactory {

    private final CapabilityValueSupport capabilityValueSupport;

    public ControlCommandEntity createManual(
            DeviceEntity device,
            String target,
            Object value,
            UserEntity actor,
            String actorName
    ) {
        ControlCommandEntity command = base(device, target, value);
        command.setActor(actor);
        command.setActorName(actorName);
        command.setSource("manual");
        return command;
    }

    public ControlCommandEntity createWithActorName(
            DeviceEntity device,
            String target,
            Object value,
            String actorName
    ) {
        return createWithSource(device, target, value, actorName, "system");
    }

    public ControlCommandEntity createWithSource(
            DeviceEntity device,
            String target,
            Object value,
            String actorName,
            String source
    ) {
        ControlCommandEntity command = base(device, target, value);
        command.setActorName(actorName);
        command.setSource(normalizeSource(source));
        return command;
    }

    public ControlCommandEntity createSystem(
            DeviceEntity device,
            String target,
            Object value
    ) {
        return createWithSource(device, target, value, "SYSTEM", "system");
    }

    private ControlCommandEntity base(DeviceEntity device, String target, Object value) {
        ControlCommandEntity command = new ControlCommandEntity();
        command.setDevice(device);
        command.setTarget(target);
        capabilityValueSupport.assignCommandValue(command, value);
        command.setStatus(CommandStatus.PENDING);
        return command;
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "system";
        }
        return source.trim().toLowerCase();
    }
}
