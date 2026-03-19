package com.java.domain.provider;

import com.java.controller.dto.AuditEventItem;

public interface AuditEventVisibilityPolicy {
    boolean shouldDisplay(AuditEventItem item);
}