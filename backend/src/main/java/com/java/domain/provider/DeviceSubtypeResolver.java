package com.java.domain.provider;

import com.java.domain.DeviceClass;
public interface DeviceSubtypeResolver {
    String normalize(String subtype);
    DeviceClass resolveDeviceClass(String subtype);
    boolean isSupportedMonitoringSubtype(String subtype);
}