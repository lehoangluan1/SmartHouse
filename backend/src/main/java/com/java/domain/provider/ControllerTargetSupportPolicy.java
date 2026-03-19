package com.java.domain.provider;

import com.java.domain.DeviceTarget;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ControllerTargetSupportPolicy implements DeviceTargetSupportPolicy {

    private final DeviceSubtypeResolver subtypeResolver;

    public ControllerTargetSupportPolicy(DeviceSubtypeResolver subtypeResolver) {
        this.subtypeResolver = subtypeResolver;
    }

    @Override
    public boolean supports(String subtype) {
        String normalized = subtypeResolver.normalize(subtype);
        return "SMART_CONTROLLER".equals(normalized) || "CONTROLLER".equals(normalized);
    }

    @Override
    public Set<DeviceTarget> supportedTargets() {
        return EnumSet.of(DeviceTarget.MODE);
    }
}