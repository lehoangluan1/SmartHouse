package com.java.mapper;

import com.java.controller.dto.AuditEventItem;
import com.java.domain.provider.CommandAuditProvider;
import com.java.domain.service.AuditStateResolver;
import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.AuditResolvedState;
import com.java.domain.service.dto.CommandAuditContext;
import com.java.persistence.entity.ControlCommandEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandAuditEventMapper {

    private final AuditStateResolver auditStateResolver;
    private final AuditValueFormatter auditValueFormatter;
    private final CommandAuditProvider commandAuditProvider;

    public AuditEventItem map(ControlCommandEntity entity) {
        CommandAuditContext context = commandAuditProvider.provide(entity);

        AuditResolvedState resolvedState = auditStateResolver.resolve(
                entity.getDevice() != null ? entity.getDevice().getId() : null,
                context.capabilityCode(),
                entity.getCreatedAt(),
                context.normalizedTarget(),
                "-",
                context.toState(),
                context.fallbackDetails()
        );

        return AuditEventItem.builder()
                .source("COMMAND")
                .id(entity.getId())
                .category("device")
                .type(resolvedState.type())
                .status(entity.getStatus() != null ? entity.getStatus().name() : "PENDING")
                .homeId(entity.getDevice() != null && entity.getDevice().getHome() != null
                        ? entity.getDevice().getHome().getId()
                        : null)
                .deviceId(entity.getDevice() != null ? entity.getDevice().getId() : null)
                .deviceName(entity.getDevice() != null ? entity.getDevice().getName() : null)
                .userId(entity.getActor() != null ? entity.getActor().getId() : null)
                .username(auditValueFormatter.nonBlank(
                        entity.getActor() != null ? entity.getActor().getUsername() : null,
                        entity.getActorName(),
                        "System"
                ))
                .method("COMMAND")
                .fromState(resolvedState.fromState())
                .toState(resolvedState.toState())
                .details(resolvedState.details())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}