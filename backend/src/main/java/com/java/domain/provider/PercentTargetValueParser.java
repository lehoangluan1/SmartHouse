package com.java.domain.provider;

import com.java.config.BadRequestException;
import com.java.domain.DeviceTarget;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PercentTargetValueParser implements DeviceTargetValueParser {

    private static final Set<DeviceTarget> SUPPORTED = Set.of(
            DeviceTarget.SPEED,
            DeviceTarget.BRIGHTNESS
    );

    @Override
    public boolean supports(DeviceTarget target) {
        return SUPPORTED.contains(target);
    }

    @Override
    public Object parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException("Value cannot be empty");
        }

        try {
            double number = Double.parseDouble(rawValue.trim());
            if (number < 0 || number > 100) {
                throw new BadRequestException("Intensity value must be between 0..100");
            }
            return number;
        } catch (BadRequestException e) {
            throw e;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid number value: " + rawValue);
        }
    }
}