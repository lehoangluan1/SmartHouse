package com.java.domain.provider;

import com.java.config.BadRequestException;
import com.java.domain.DeviceTarget;
import com.java.domain.SystemMode;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ModeTargetValueParser implements DeviceTargetValueParser {

    @Override
    public boolean supports(DeviceTarget target) {
        return DeviceTarget.MODE == target;
    }

    @Override
    public Object parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException("Mode cannot be empty");
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);

        SystemMode mode = switch (normalized) {
            case "auto" -> SystemMode.auto;
            case "manual" -> SystemMode.manual;
            case "sleep" -> SystemMode.sleep;
            case "away" -> SystemMode.away;
            default -> throw new BadRequestException("Invalid mode: " + rawValue);
        };

        return mode.name().toLowerCase(Locale.ROOT);
    }
}