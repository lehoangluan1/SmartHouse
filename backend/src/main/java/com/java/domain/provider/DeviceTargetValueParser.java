package com.java.domain.provider;
import com.java.domain.DeviceTarget;

public interface DeviceTargetValueParser {
    boolean supports(DeviceTarget target);
    Object parse(String rawValue);
}