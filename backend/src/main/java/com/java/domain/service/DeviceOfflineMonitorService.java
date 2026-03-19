package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.domain.AlertType;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceOfflineMonitorService {

    private final DeviceRepository deviceRepository;
    private final OfflineDetector offlineDetector;
    private final AlertLifecycleService alertLifecycleService;

    @Transactional
    public void syncOfflineStates() {
        OffsetDateTime now = OffsetDateTime.now();
        List<DeviceEntity> devices = deviceRepository.findAll();

        for (DeviceEntity device : devices) {
            syncDeviceOfflineState(device, now);
        }
    }

    private void syncDeviceOfflineState(DeviceEntity device, OffsetDateTime now) {
        boolean offline = offlineDetector.isOffline(
                device.getId(),
                device.getLastSeen(),
                now
        );

        if (offline) {
            handleOffline(device);
        } else {
            handleOnline(device);
        }
    }

    private void handleOffline(DeviceEntity device) {
        updateOnlineFlag(device, false);

        alertLifecycleService.upsertActiveAlert(
                device.getId(),
                null,
                AlertType.DEVICE_OFFLINE,
                "Device offline: " + device.getName()
        );
    }

    private void handleOnline(DeviceEntity device) {
        updateOnlineFlag(device, true);

        alertLifecycleService.resolveIfExists(
                device.getId(),
                null,
                AlertType.DEVICE_OFFLINE
        );
    }

    private void updateOnlineFlag(DeviceEntity device, boolean online) {
        if (Boolean.valueOf(online).equals(device.getIsOnline())) {
            return;
        }

        device.setIsOnline(online);
        deviceRepository.save(device);
    }
}