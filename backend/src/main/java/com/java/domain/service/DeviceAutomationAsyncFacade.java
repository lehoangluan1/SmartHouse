package com.java.domain.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DeviceAutomationAsyncFacade {

    private final DeviceAutomationWorker deviceAutomationWorker;

    public DeviceAutomationAsyncFacade(DeviceAutomationWorker deviceAutomationWorker) {
        this.deviceAutomationWorker = deviceAutomationWorker;
    }

    @Async("automationExecutor")
    public CompletableFuture<Void> evaluateOneDeviceAsync(Long deviceId) {
        return evaluateOneDeviceAsync(deviceId, "scheduler");
    }

    @Async("automationExecutor")
    public CompletableFuture<Void> evaluateOneDeviceAsync(Long deviceId, String reason) {
        deviceAutomationWorker.evaluateAndApplyOneDevice(deviceId, reason);
        return CompletableFuture.completedFuture(null);
    }
}
