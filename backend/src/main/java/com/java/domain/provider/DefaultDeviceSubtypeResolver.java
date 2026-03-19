package com.java.domain.provider;

import com.java.domain.DeviceClass;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DefaultDeviceSubtypeResolver implements DeviceSubtypeResolver {

    private static final Set<String> SENSOR_SUBTYPES = Set.of(
            "TEMPERATURE_NODE", "HUMIDITY_NODE", "LIGHT_NODE", "MOTION_NODE"
    );

    private static final Set<String> ACTUATOR_SUBTYPES = Set.of(
            "FAN", "LIGHT", "AIR_CONDITIONER"
    );

    private static final Set<String> CONTROLLER_SUBTYPES = Set.of(
            "SMART_CONTROLLER", "CONTROLLER"
    );

    @Override
    public String normalize(String subtype) {
        return subtype == null ? "" : subtype.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public DeviceClass resolveDeviceClass(String subtype) {
        String normalized = normalize(subtype);

        if (SENSOR_SUBTYPES.contains(normalized)) {
            return DeviceClass.SENSOR_NODE;
        }
        if (ACTUATOR_SUBTYPES.contains(normalized)) {
            return DeviceClass.ACTUATOR;
        }
        if (CONTROLLER_SUBTYPES.contains(normalized)) {
            return DeviceClass.CONTROLLER;
        }
        return DeviceClass.OTHER;
    }

    @Override
    public boolean isSupportedMonitoringSubtype(String subtype) {
        return SENSOR_SUBTYPES.contains(normalize(subtype));
    }
}