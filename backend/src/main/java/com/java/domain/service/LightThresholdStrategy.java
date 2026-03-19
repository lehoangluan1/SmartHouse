package com.java.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.SensorEntity;

@Component
public class LightThresholdStrategy implements ThresholdRuleStrategy {

    @Override
    public boolean supports(String sensorType) {
        return "LIGHT".equalsIgnoreCase(sensorType);
    }

    @Override
    public Optional<EvaluationResult> evaluate(SensorEntity sensor, ConfigEntity config, Object value) {
        int v = (int) Math.round(Double.parseDouble(String.valueOf(value)));

        if (config.getLlow() != null && v <= config.getLlow()) {
            return Optional.of(
                new EvaluationResult(
                    "LOW_LIGHT",
                    "Low light at " + sensor.getName() + ": " + v,
                    true
                )
            );
        }

        if (config.getLhigh() != null && v >= config.getLhigh()) {
            return Optional.of(
                new EvaluationResult(
                    "HIGH_LIGHT",
                    "High light at " + sensor.getName() + ": " + v,
                    true
                )
            );
        }

        return Optional.empty();
    }
}