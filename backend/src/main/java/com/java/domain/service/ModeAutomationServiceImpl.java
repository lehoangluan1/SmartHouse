package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.persistence.repo.DeviceRepository;

@Service
public class ModeAutomationServiceImpl implements ModeAutomationService {

    private final DeviceRepository deviceRepository;
    private final DeviceAutomationAsyncFacade deviceAutomationAsyncFacade;

    public ModeAutomationServiceImpl(
            DeviceRepository deviceRepository,
            DeviceAutomationAsyncFacade deviceAutomationAsyncFacade
    ) {
        this.deviceRepository = deviceRepository;
        this.deviceAutomationAsyncFacade = deviceAutomationAsyncFacade;
    }

    @Override
    public void evaluateAndApply(Long deviceId) {
        evaluateAndApply(deviceId, "scheduler");
    }

    @Override
    public void evaluateAndApply(Long deviceId, String reason) {
        deviceAutomationAsyncFacade.evaluateOneDeviceAsync(deviceId, reason);
    }

    @Override
    public void evaluateAllByHome(Long homeId) {
        evaluateAllByHome(homeId, "scheduler");
    }

    @Override
    public void evaluateAllByHome(Long homeId, String reason) {
        deviceRepository.findByHomeId(homeId).forEach(device ->
                deviceAutomationAsyncFacade.evaluateOneDeviceAsync(device.getId(), reason)
        );
    }
}
