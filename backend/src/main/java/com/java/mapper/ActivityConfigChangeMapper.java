package com.java.mapper;

import com.java.controller.dto.AuditConfigChangeItem;
import com.java.domain.service.AuditValueFormatter;
import com.java.persistence.entity.ActivityLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityConfigChangeMapper {

    private final AuditValueFormatter auditValueFormatter;

    public AuditConfigChangeItem map(ActivityLogEntity entity) {
        return AuditConfigChangeItem.builder()
                .source("ACTIVITY_LOG")
                .id(entity.getId())
                .homeId(entity.getHome() != null ? entity.getHome().getId() : null)
                .deviceId(entity.getDevice() != null ? entity.getDevice().getId() : null)
                .deviceName(entity.getDevice() != null ? entity.getDevice().getName() : null)
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .username(entity.getUser() != null ? entity.getUser().getUsername() : "System")
                .prevConfig(auditValueFormatter.displayRaw(entity.getOldValue()))
                .newConfig(auditValueFormatter.displayRaw(entity.getNewValue()))
                .reason(auditValueFormatter.nonBlank(
                        auditValueFormatter.displayRaw(entity.getDetail()),
                        entity.getAction(),
                        entity.getMethod(),
                        ""
                ))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}