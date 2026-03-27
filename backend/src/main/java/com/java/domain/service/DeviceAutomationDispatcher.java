package com.java.domain.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceAutomationDispatcher {

    private final DeviceAutomationAsyncFacade asyncFacade;
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public void dispatch(Long deviceId) {
        if (!inFlight.add(deviceId)) {
            log.debug("AUTO_DISPATCH_SKIP_INFLIGHT deviceId={}", deviceId);
            return;
        }

        asyncFacade.evaluateOneDeviceAsync(deviceId)
                .whenComplete((r, ex) -> {
                    inFlight.remove(deviceId);
                    if (ex != null) {
                        log.warn("AUTO_DEVICE_ASYNC_FAILED deviceId={}", deviceId, ex);
                    }
                });
    }
}