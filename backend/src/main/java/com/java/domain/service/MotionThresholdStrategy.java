package com.java.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.SensorEntity;

@Component
public class MotionThresholdStrategy implements ThresholdRuleStrategy {

    @Override
    public boolean supports(String sensorType) {
        return "MOTION".equalsIgnoreCase(sensorType);
    }

    @Override
    public Optional<EvaluationResult> evaluate(SensorEntity sensor, ConfigEntity config, Object value) {
        boolean detected = toBoolean(value);

        if (detected) {
            return Optional.of(
                new EvaluationResult(
                    "MOTION_DETECTED",
                    "Motion detected at " + sensor.getName(),
                    true
                )
            );
        }

        return Optional.empty();
    }

    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Boolean b) {
            return b;
        }

        if (value instanceof Number n) {
            return n.intValue() != 0;
        }

        String text = String.valueOf(value).trim().toLowerCase();

        return "1".equals(text)
                || "true".equals(text)
                || "on".equals(text)
                || "yes".equals(text)
                || "detected".equals(text)
                || "motion".equals(text);
    }
}