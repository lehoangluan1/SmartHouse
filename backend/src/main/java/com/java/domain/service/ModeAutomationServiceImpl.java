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
        deviceAutomationAsyncFacade.evaluateOneDeviceAsync(deviceId);
    }

    @Override
    public void evaluateAllByHome(Long homeId) {
        deviceRepository.findByHomeId(homeId).forEach(device ->
                deviceAutomationAsyncFacade.evaluateOneDeviceAsync(device.getId())
        );
    }
}