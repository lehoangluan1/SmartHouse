package com.java.domain.provider;

import com.java.config.BadRequestException;
import com.java.domain.DeviceTarget;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PowerTargetValueParser implements DeviceTargetValueParser {

    @Override
    public boolean supports(DeviceTarget target) {
        return DeviceTarget.POWER == target;
    }

    @Override
    public Object parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException("Value cannot be empty");
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "on", "1", "true" -> true;
            case "off", "0", "false" -> false;
            default -> throw new BadRequestException("Invalid boolean value: " + rawValue);
        };
    }
}