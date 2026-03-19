package com.java.domain.provider;

import com.java.domain.service.dto.AuditQuery;
import com.java.domain.service.dto.AuditSourceResult;

public interface AuditSourceProvider {
    AuditSourceResult fetch(AuditQuery query);
}