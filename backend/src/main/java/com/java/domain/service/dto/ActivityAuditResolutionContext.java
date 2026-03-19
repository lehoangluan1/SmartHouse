package com.java.domain.service.dto;

import com.java.persistence.entity.ActivityLogEntity;

public record ActivityAuditResolutionContext(
        ActivityLogEntity entity,
        AuditParsedDetail parsedDetail,
        String category,
        String action,
        Long deviceId
) {
}