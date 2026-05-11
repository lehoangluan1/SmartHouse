package com.java.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.java.domain.SystemMode;
import com.java.domain.service.dto.AutomationDecision;
import com.java.persistence.entity.ConfigEntity;

class FanAutomationPolicyTest {

    private final FanAutomationPolicy policy = new FanAutomationPolicy();

    @Test
    void turnsFanOnImmediatelyWhenAutoTempExceedsHighThreshold() {
        ConfigEntity config = config();

        List<AutomationDecision> decisions = policy.decide(Map.of(), config, 31.0, SystemMode.auto);

        assertThat(decisions).contains(
                new AutomationDecision("POWER", "true", "AUTO_TEMP_HIGH"),
                new AutomationDecision("SPEED", "70", "AUTO_TEMP_HIGH")
        );
    }

    @Test
    void manualModeDoesNotOverrideFan() {
        ConfigEntity config = config();

        List<AutomationDecision> decisions = policy.decide(Map.of(), config, 31.0, SystemMode.manual);

        assertThat(decisions).isEmpty();
    }

    private ConfigEntity config() {
        ConfigEntity config = new ConfigEntity();
        config.setThigh(30.0);
        config.setTlow(27.0);
        config.setAutoFanSpeed(70);
        return config;
    }
}
