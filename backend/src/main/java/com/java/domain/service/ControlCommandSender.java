package com.java.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.adapter.DeviceCommandAdapter;
import com.java.config.BadRequestException;
import com.java.persistence.entity.ControlCommandEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.ControlCommandRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ControlCommandSender {

    private final List<DeviceCommandAdapter> adapters;
    private final ControlCommandRepository controlCommandRepository;

    @Transactional
    public ControlCommandEntity sendNow(ControlCommandEntity command) {
        DeviceEntity device = command.getDevice();

        DeviceCommandAdapter adapter = adapters.stream()
                .filter(a -> a.supports(device.getDeviceKey()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No suitable adapter found for device"));

        log.info("COMMAND dispatch attempted method=long-poll id={}", command.getId());
        var result = adapter.send(command);

        if (result.success()) {
            log.info("COMMAND dispatch signalled id={} status={}", command.getId(), command.getStatus());
        } else {
            log.warn("COMMAND dispatch fail id={} message={}", command.getId(), result.message());
        }

        return controlCommandRepository.save(command);
    }
}
