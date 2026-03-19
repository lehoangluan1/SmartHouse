package com.java.domain.service;
import com.java.config.BadRequestException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DefaultModeValueResolver implements ModeValueResolver {

    private static final Set<String> SUPPORTED_MODES = Set.of("auto", "manual", "sleep", "away");

    @Override
    public String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new BadRequestException("mode là bắt buộc");
        }

        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        validateNormalizedMode(normalized);
        return normalized;
    }

    @Override
    public void validateNormalizedMode(String mode) {
        if (mode == null || mode.isBlank() || !SUPPORTED_MODES.contains(mode)) {
            throw new BadRequestException("mode must be one of: auto, manual, sleep, away");
        }
    }
}