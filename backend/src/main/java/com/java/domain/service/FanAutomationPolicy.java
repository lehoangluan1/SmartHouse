package com.java.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.java.domain.SystemMode;
import com.java.domain.service.dto.AutomationDecision;
import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;

@Component
public class FanAutomationPolicy {

    private static final String CAPABILITY_POWER = "POWER";
    private static final String CAPABILITY_SPEED = "SPEED";

    private static final int DEFAULT_SLEEP_FAN_SPEED = 30;
    private static final int DEFAULT_AUTO_FAN_SPEED = 70;

    public List<AutomationDecision> decide(
            Map<String, DeviceRuntimeStateEntity> fanStateMap,
            ConfigEntity config,
            Double temp,
            SystemMode mode
    ) {
        List<AutomationDecision> decisions = new ArrayList<>();

        if (temp == null || config == null || mode == null) {
            return decisions;
        }

        Boolean fanOn = readBoolean(fanStateMap, CAPABILITY_POWER);
        Integer fanSpeed = readInteger(fanStateMap, CAPABILITY_SPEED);

        switch (mode) {
            case auto -> {
                if (config.getThigh() != null && temp > config.getThigh()) {
                    ensureFan(decisions, fanOn, fanSpeed, true, resolveAutoFanSpeed(config), "AUTO_TEMP_HIGH");
                } else if (config.getTlow() != null && temp < config.getTlow()) {
                    ensureFan(decisions, fanOn, fanSpeed, false, null, "AUTO_TEMP_LOW");
                }
            }
            case sleep -> {
                if (config.getTsleepHigh() != null && temp > config.getTsleepHigh()) {
                    ensureFan(decisions, fanOn, fanSpeed, true, resolveSleepFanSpeed(config), "SLEEP_TEMP_HIGH");
                } else if (config.getTsleepLow() != null && temp < config.getTsleepLow()) {
                    ensureFan(decisions, fanOn, fanSpeed, false, null, "SLEEP_TEMP_LOW");
                }
            }
            case away -> {
                if (config.getTawayHigh() != null && temp > config.getTawayHigh()) {
                    ensureFan(decisions, fanOn, fanSpeed, true, resolveAutoFanSpeed(config), "AWAY_TEMP_HIGH");
                } else {
                    ensureFan(decisions, fanOn, fanSpeed, false, null, "AWAY_TEMP_NORMAL");
                }
            }
            case manual -> {
                // manual mode does not require automatic intervention
            }
        }

        return decisions;
    }

    private void ensureFan(
            List<AutomationDecision> decisions,
            Boolean currentFanOn,
            Integer currentFanSpeed,
            boolean desiredOn,
            Integer desiredSpeed,
            String reason
    ) {
        if (desiredOn) {
            if (!Boolean.TRUE.equals(currentFanOn)) {
                decisions.add(new AutomationDecision(CAPABILITY_POWER, "true", reason));
            }
            if (desiredSpeed != null && !desiredSpeed.equals(currentFanSpeed)) {
                decisions.add(new AutomationDecision(
                        CAPABILITY_SPEED,
                        String.valueOf(clampSpeed(desiredSpeed)),
                        reason
                ));
            }
            return;
        }

        if (!Boolean.FALSE.equals(currentFanOn)) {
            decisions.add(new AutomationDecision(CAPABILITY_POWER, "false", reason));
        }
    }

    private int resolveAutoFanSpeed(ConfigEntity config) {
        Integer speed = config.getAutoFanSpeed();
        return clampSpeed(speed != null ? speed : DEFAULT_AUTO_FAN_SPEED);
    }

    private int resolveSleepFanSpeed(ConfigEntity config) {
        Integer speed = config.getSleepFanSpeed();
        return clampSpeed(speed != null ? speed : DEFAULT_SLEEP_FAN_SPEED);
    }

    private int clampSpeed(Integer speed) {
        if (speed == null) {
            return DEFAULT_SLEEP_FAN_SPEED;
        }
        return Math.max(1, Math.min(100, speed));
    }

    private Boolean readBoolean(Map<String, DeviceRuntimeStateEntity> stateMap, String capabilityCode) {
        if (stateMap == null) {
            return null;
        }
        DeviceRuntimeStateEntity state = stateMap.get(capabilityCode);
        return state != null ? state.getValueBoolean() : null;
    }

    private Integer readInteger(Map<String, DeviceRuntimeStateEntity> stateMap, String capabilityCode) {
        if (stateMap == null) {
            return null;
        }
        DeviceRuntimeStateEntity state = stateMap.get(capabilityCode);
        if (state == null || state.getValueNumber() == null) {
            return null;
        }
        return state.getValueNumber().intValue();
    }
}