package com.java.domain.provider;
import com.java.domain.DeviceTarget;

public interface DeviceTargetResolver {
    DeviceTarget resolve(String rawTarget);
    String normalize(String rawTarget);
}
