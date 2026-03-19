package com.java.domain.provider;

import com.java.domain.service.dto.CommandAuditContext;
import com.java.persistence.entity.ControlCommandEntity;

public interface CommandAuditProvider {
    CommandAuditContext provide(ControlCommandEntity entity);
}