package com.java.domain.service.dto;

import com.java.controller.dto.AuditConfigChangeItem;
import com.java.controller.dto.AuditEventItem;
import java.util.List;

public record AuditSourceResult(
        List<AuditConfigChangeItem> configChanges,
        List<AuditEventItem> events
) {
    public static AuditSourceResult of(
            List<AuditConfigChangeItem> configChanges,
            List<AuditEventItem> events
    ) {
        return new AuditSourceResult(configChanges, events);
    }
}