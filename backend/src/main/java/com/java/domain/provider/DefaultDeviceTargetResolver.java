package com.java.domain.provider;

import com.java.config.BadRequestException;
import com.java.domain.DeviceTarget;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DefaultDeviceTargetResolver implements DeviceTargetResolver {

    private static final Map<String, DeviceTarget> TARGET_ALIASES = Map.ofEntries(
            Map.entry("mode", DeviceTarget.MODE),

            Map.entry("fan", DeviceTarget.POWER),
            Map.entry("fanstatus", DeviceTarget.POWER),
            Map.entry("fan_status", DeviceTarget.POWER),
            Map.entry("light", DeviceTarget.POWER),
            Map.entry("lightstatus", DeviceTarget.POWER),
            Map.entry("light_status", DeviceTarget.POWER),
            Map.entry("power", DeviceTarget.POWER),

            Map.entry("fanspeed", DeviceTarget.SPEED),
            Map.entry("fan_speed", DeviceTarget.SPEED),
            Map.entry("fan-speed", DeviceTarget.SPEED),
            Map.entry("speed", DeviceTarget.SPEED),

            Map.entry("lightlevel", DeviceTarget.BRIGHTNESS),
            Map.entry("light_level", DeviceTarget.BRIGHTNESS),
            Map.entry("light-level", DeviceTarget.BRIGHTNESS),
            Map.entry("brightness", DeviceTarget.BRIGHTNESS),

            Map.entry("temperature", DeviceTarget.TEMPERATURE),
            Map.entry("temp", DeviceTarget.TEMPERATURE),

            Map.entry("humidity", DeviceTarget.HUMIDITY),
            Map.entry("humid", DeviceTarget.HUMIDITY),

            Map.entry("motion", DeviceTarget.MOTION),
            Map.entry("presence", DeviceTarget.MOTION),
            Map.entry("pir", DeviceTarget.MOTION)
    );

    @Override
    public DeviceTarget resolve(String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) {
            throw new BadRequestException("Target cannot be empty");
        }

        String normalized = rawTarget.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");

        DeviceTarget target = TARGET_ALIASES.get(normalized);
        if (target == null) {
            throw new BadRequestException("Invalid target: " + rawTarget);
        }

        return target;
    }

    @Override
    public String normalize(String rawTarget) {
        return resolve(rawTarget).name();
    }
}