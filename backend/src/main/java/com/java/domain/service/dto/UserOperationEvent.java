package com.java.domain.service.dto;
import java.time.OffsetDateTime;
import java.util.Map;

public record UserOperationEvent(
        String eventType,
        Long homeId,
        Long userId,
        String username,
        String roleInHome,
        Boolean allowProfileActivation,
        Boolean primary,
        String provider,
        Long actorUserId,
        OffsetDateTime occurredAt,
        Map<String, Object> metadata
) {
}