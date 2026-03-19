package com.java.domain.provider;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DefaultCapabilityCodeResolver implements CapabilityCodeResolver {

    private static final Set<String> SUPPORTED = Set.of(
            "MODE",
            "POWER",
            "SPEED",
            "BRIGHTNESS"
    );

    @Override
    public String resolve(String rawTarget, String fallbackType) {
        String candidate = normalize(rawTarget);
        if (SUPPORTED.contains(candidate)) {
            return candidate;
        }

        candidate = normalize(fallbackType);
        if (SUPPORTED.contains(candidate)) {
            return candidate;
        }

        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}