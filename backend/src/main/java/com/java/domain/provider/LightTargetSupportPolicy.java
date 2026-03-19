package com.java.domain.provider;

import com.java.domain.DeviceTarget;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LightTargetSupportPolicy implements DeviceTargetSupportPolicy {

    private final DeviceSubtypeResolver subtypeResolver;

    public LightTargetSupportPolicy(DeviceSubtypeResolver subtypeResolver) {
        this.subtypeResolver = subtypeResolver;
    }

    @Override
    public boolean supports(String subtype) {
        return "LIGHT".equals(subtypeResolver.normalize(subtype));
    }

    @Override
    public Set<DeviceTarget> supportedTargets() {
        return EnumSet.of(DeviceTarget.POWER, DeviceTarget.BRIGHTNESS);
    }
}