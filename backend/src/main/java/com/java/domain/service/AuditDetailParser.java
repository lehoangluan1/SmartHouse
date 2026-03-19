package com.java.domain.service;

import com.java.domain.service.dto.AuditParsedDetail;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class AuditDetailParser {

    private static final String CAPABILITY_MODE = "MODE";

    private final ObjectMapper objectMapper;
    private final AuditValueFormatter auditValueFormatter;

    public AuditParsedDetail parse(Object rawDetail) {
        if (rawDetail == null) {
            return AuditParsedDetail.empty();
        }

        if (rawDetail instanceof Map<?, ?> rawMap) {
            return parseMap(rawMap);
        }

        String text = String.valueOf(rawDetail);
        if (text.isBlank() || "-".equals(text)) {
            return AuditParsedDetail.empty();
        }

        try {
            Map<String, Object> map = objectMapper.readValue(
                    text,
                    new TypeReference<Map<String, Object>>() {}
            );
            return parseMap(map);
        } catch (JacksonException ignored) {
            return AuditParsedDetail.withDescription(text);
        }
    }

    private AuditParsedDetail parseMap(Map<?, ?> rawMap) {
        String target = asString(rawMap.get("target"));

        Object fromState = firstNonNull(
                rawMap.get("fromState"),
                rawMap.get("previousValue"),
                rawMap.get("oldValue"),
                rawMap.get("from"),
                rawMap.get("previousMode")
        );

        Object toState = firstNonNull(
                rawMap.get("toState"),
                rawMap.get("value"),
                rawMap.get("newValue"),
                rawMap.get("restoredMode")
        );

        Object previousValue = firstNonNull(
                rawMap.get("previousValue"),
                rawMap.get("oldValue"),
                rawMap.get("from"),
                rawMap.get("fromState"),
                rawMap.get("previousMode")
        );

        Object value = firstNonNull(
                rawMap.get("value"),
                rawMap.get("newValue"),
                rawMap.get("toState"),
                rawMap.get("restoredMode")
        );

        Object actorName = rawMap.get("actorName");
        Object holdUntil = rawMap.get("holdUntil");
        Object holdMinutes = rawMap.get("holdMinutes");
        Object expiredAt = rawMap.get("expiredAt");
        Object restoredMode = rawMap.get("restoredMode");

        if ((restoredMode != null || expiredAt != null) && auditValueFormatter.isBlankTarget(target)) {
            target = CAPABILITY_MODE;
        }

        return AuditParsedDetail.builder()
                .target(target)
                .value(value)
                .previousValue(previousValue)
                .fromState(fromState)
                .toState(toState)
                .description(resolveDescription(rawMap, target, actorName, holdUntil, holdMinutes, expiredAt, restoredMode))
                .build();
    }

    private String resolveDescription(
            Map<?, ?> rawMap,
            String target,
            Object actorName,
            Object holdUntil,
            Object holdMinutes,
            Object expiredAt,
            Object restoredMode
    ) {
        String explicitDetails = asString(rawMap.get("details"));
        if (explicitDetails != null && !explicitDetails.isBlank() && !"-".equals(explicitDetails)) {
            return explicitDetails;
        }

        if (actorName != null || holdUntil != null || holdMinutes != null) {
            String description = "Manual hold started";
            if (holdMinutes != null) {
                description += " (" + holdMinutes + " min)";
            }
            return description;
        }

        if (restoredMode != null || expiredAt != null) {
            return "Manual hold restored";
        }

        if (!auditValueFormatter.isBlankTarget(target)) {
            String normalizedTarget = auditValueFormatter.normalizeTarget(target);
            return "MODE".equals(normalizedTarget)
                    ? "Mode changed"
                    : normalizedTarget + " changed";
        }

        return null;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}