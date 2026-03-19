package com.java.domain.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThresholdStrategyFactory {
    private final List<ThresholdRuleStrategy> strategies;

    public ThresholdRuleStrategy resolve(String sensorType) {
        return strategies.stream()
                .filter(s -> s.supports(sensorType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sensor type not supported: " + sensorType));
    }
}
