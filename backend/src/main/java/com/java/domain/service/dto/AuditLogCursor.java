package com.java.domain.service.dto;

import java.time.OffsetDateTime;

public record AuditLogCursor(
        OffsetDateTime createdAt,
        Long id
) {
}
