package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.NotFoundException;
import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.DeviceConfigEntity;
import com.java.persistence.repo.DeviceConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceConfigService {

    private final DeviceConfigRepository deviceConfigRepository;

    public ConfigEntity getConfigOfDevice(Long deviceId) {
        DeviceConfigEntity deviceConfig = deviceConfigRepository.findLatestByDeviceId(deviceId);

        if (deviceConfig == null || deviceConfig.getConfig() == null) {
            throw new NotFoundException("Device is not configured yet");
        }

        return deviceConfig.getConfig();
    }

    public ConfigEntity getActiveConfig(Long deviceId) {
        return getConfigOfDevice(deviceId);
    }
}