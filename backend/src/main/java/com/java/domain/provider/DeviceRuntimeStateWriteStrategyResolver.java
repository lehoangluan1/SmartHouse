package com.java.domain.provider;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class DeviceRuntimeStateWriteStrategyResolver {

    private final List<DeviceRuntimeStateWriteStrategy> strategies;

    public DeviceRuntimeStateWriteStrategyResolver(List<DeviceRuntimeStateWriteStrategy> strategies) {
        this.strategies = strategies;
    }

    public DeviceRuntimeStateWriteStrategy resolve(String capabilityCode) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(capabilityCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No DeviceRuntimeStateWriteStrategy found for capability: " + capabilityCode
                ));
    }
}