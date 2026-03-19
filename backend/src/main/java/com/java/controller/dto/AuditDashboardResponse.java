package com.java.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditDashboardResponse {
    private AuditSummaryResponse summary;
    private PageResponse<AuditConfigChangeItem> configChanges;
    private PageResponse<AuditEventItem> events;
}