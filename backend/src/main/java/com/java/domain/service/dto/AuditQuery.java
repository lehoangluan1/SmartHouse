package com.java.domain.service.dto;

import java.time.OffsetDateTime;

public record AuditQuery(
        Long homeId,
        OffsetDateTime from,
        OffsetDateTime to,
        int configPage,
        int configSize,
        String configKeyword,
        int eventPage,
        int eventSize,
        String eventKeyword,
        String eventCategory
) {
}