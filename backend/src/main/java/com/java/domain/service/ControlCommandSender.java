package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.adapter.DeviceCommandAdapter;
import com.java.config.BadRequestException;
import com.java.domain.CommandStatus;
import com.java.domain.events.ControlCommandEvent;
import com.java.eventing.DomainEventBus;
import com.java.persistence.entity.ControlCommandEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.ControlCommandRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ControlCommandSender {

    private final List<DeviceCommandAdapter> adapters;
    private final ControlCommandRepository controlCommandRepository;
    private final DomainEventBus eventBus;

    @Transactional
    public ControlCommandEntity sendNow(ControlCommandEntity command) {
        DeviceEntity device = command.getDevice();

        DeviceCommandAdapter adapter = adapters.stream()
                .filter(a -> a.supports(device.getDeviceKey()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No suitable adapter found for device"));

        var result = adapter.send(command);

        command.setStatus(result.success() ? CommandStatus.SENT : CommandStatus.FAILED);
        command.setSentAt(OffsetDateTime.now());

        ControlCommandEntity saved = controlCommandRepository.save(command);

        eventBus.publish(new ControlCommandEvent(
                saved.getId(),
                device.getHome() != null ? device.getHome().getId() : null,
                device.getId(),
                device.getDeviceKey(),
                saved.getTarget(),
                extractValue(saved),
                saved.getActorName()
        ));

        return saved;
    }

    private String extractValue(ControlCommandEntity command) {
        if (command.getValueBoolean() != null) {
            return String.valueOf(command.getValueBoolean());
        }
        if (command.getValueNumber() != null) {
            return String.valueOf(command.getValueNumber());
        }
        return command.getValueText();
    }
}