package com.java.domain.provider;

import com.java.domain.DeviceTarget;
import java.util.Set;

public interface DeviceTargetSupportPolicy {
    boolean supports(String subtype);
    Set<DeviceTarget> supportedTargets();
}