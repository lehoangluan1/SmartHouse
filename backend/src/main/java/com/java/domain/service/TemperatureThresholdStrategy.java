package com.java.domain.service;

import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.SensorEntity;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TemperatureThresholdStrategy implements ThresholdRuleStrategy {
    @Override
    public boolean supports(String sensorType) {
        return "TEMPERATURE".equalsIgnoreCase(sensorType);
    }

    @Override
    public Optional<EvaluationResult> evaluate(SensorEntity sensor, ConfigEntity config, Object value) {
        double v = Double.parseDouble(String.valueOf(value));
        if (v >= config.getTcritical()) {
            return Optional.of(new EvaluationResult("CRITICAL_TEMP", "Dangerous temperature at " + sensor.getName() + ": " + v, true));
        }
        return Optional.empty();
    }
}
