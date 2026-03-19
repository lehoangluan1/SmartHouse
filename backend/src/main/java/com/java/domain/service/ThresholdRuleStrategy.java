package com.java.domain.service;

import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.SensorEntity;
import java.util.Optional;

public interface ThresholdRuleStrategy {
    boolean supports(String sensorType);
    Optional<EvaluationResult> evaluate(SensorEntity sensor, ConfigEntity config, Object value);

    record EvaluationResult(String alertType, String message, boolean shouldAutoControl) {}
}
