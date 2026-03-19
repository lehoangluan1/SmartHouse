package com.java.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.SensorEntity;

@Component
public class HumidityThresholdStrategy implements ThresholdRuleStrategy {
    @Override
    public boolean supports(String sensorType) {
        return "HUMIDITY".equalsIgnoreCase(sensorType);
    }

    @Override
    public Optional<EvaluationResult> evaluate(SensorEntity sensor, ConfigEntity config, Object value) {
        return Optional.empty();
    }
}