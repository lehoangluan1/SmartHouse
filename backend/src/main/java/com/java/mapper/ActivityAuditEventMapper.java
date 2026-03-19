package com.java.mapper;

import com.java.controller.dto.AuditEventItem;
import com.java.domain.provider.ActivityCategoryResolver;
import com.java.domain.provider.ActivityStatusResolver;
import com.java.domain.service.AuditDetailParser;
import com.java.domain.service.AuditEventDetailsRefiner;
import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.ActivityAuditResolutionContext;
import com.java.domain.service.dto.AuditParsedDetail;
import com.java.domain.service.dto.AuditResolvedState;
import com.java.persistence.entity.ActivityLogEntity;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.java.domain.provider.ActivityEventStateResolver;

@Component
@RequiredArgsConstructor
public class ActivityAuditEventMapper {

    private final ActivityCategoryResolver categoryResolver;
    private final ActivityStatusResolver statusResolver;
    private final AuditDetailParser auditDetailParser;
    private final ActivityEventStateResolver activityEventStateResolver;
    private final AuditEventDetailsRefiner detailsRefiner;
    private final AuditValueFormatter auditValueFormatter;

    public AuditEventItem map(ActivityLogEntity entity) {
        String category = categoryResolver.resolve(entity);
        AuditParsedDetail parsedDetail = auditDetailParser.parse(entity.getDetail());
        String action = auditValueFormatter.safe(entity.getAction()).toUpperCase();
        Long deviceId = entity.getDevice() != null ? entity.getDevice().getId() : null;

        ActivityAuditResolutionContext context = new ActivityAuditResolutionContext(
                entity,
                parsedDetail,
                category,
                action,
                deviceId
        );

        AuditResolvedState resolvedState = activityEventStateResolver.resolve(context);

        return AuditEventItem.builder()
                .source("ACTIVITY_LOG")
                .id(entity.getId())
                .category(category)
                .type(resolvedState.type())
                .status(statusResolver.resolve(entity))
                .homeId(entity.getHome() != null ? entity.getHome().getId() : null)
                .deviceId(deviceId)
                .deviceName(entity.getDevice() != null ? entity.getDevice().getName() : null)
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .username(entity.getUser() != null ? entity.getUser().getUsername() : "System")
                .method(auditValueFormatter.nonBlank(entity.getMethod(), "-"))
                .fromState(resolvedState.fromState())
                .toState(resolvedState.toState())
                .details(detailsRefiner.refine(resolvedState))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}