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
public class LightAutomationPolicy {

    private static final String POWER_CAPABILITY = "POWER";
    private static final String POWER_ON = "true";
    private static final String POWER_OFF = "false";

    public List<AutomationDecision> decide(
            Map<String, DeviceRuntimeStateEntity> stateMap,
            ConfigEntity config,
            Double light,
            SystemMode mode
    ) {
        List<AutomationDecision> decisions = new ArrayList<>();

        if (config == null || mode == null) {
            return decisions;
        }

        boolean currentLightOn = isOn(stateMap.get(POWER_CAPABILITY));

        if (mode == SystemMode.auto) {
            if (light == null) {
                return decisions;
            }

            if (config.getLlow() != null && light < config.getLlow() && !currentLightOn) {
                decisions.add(new AutomationDecision(POWER_CAPABILITY, POWER_ON, "AUTO_LIGHT_LOW"));
            } else if (config.getLhigh() != null && light > config.getLhigh() && currentLightOn) {
                decisions.add(new AutomationDecision(POWER_CAPABILITY, POWER_OFF, "AUTO_LIGHT_HIGH"));
            }
        } else if (mode == SystemMode.sleep || mode == SystemMode.away) {
            if (currentLightOn) {
                decisions.add(new AutomationDecision(POWER_CAPABILITY, POWER_OFF, "LIGHT_MODE_FORCE_OFF"));
            }
        } else if (mode == SystemMode.manual) {
            // no-op
        }

        return decisions;
    }

    private boolean isOn(DeviceRuntimeStateEntity entity) {
        return entity != null && Boolean.TRUE.equals(entity.getValueBoolean());
    }
}
