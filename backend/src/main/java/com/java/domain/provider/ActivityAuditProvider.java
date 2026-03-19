package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.domain.service.dto.AuditQuery;
import com.java.domain.service.dto.AuditSourceResult;
import com.java.mapper.ActivityAuditEventMapper;
import com.java.mapper.ActivityConfigChangeMapper;
import com.java.persistence.repo.ActivityLogRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ActivityAuditProvider implements AuditSourceProvider {

    private final ActivityLogRepository activityLogRepository;
    private final ConfigLikeActivityDecider configLikeActivityDecider;
    private final ActivityConfigChangeMapper configChangeMapper;
    private final ActivityAuditEventMapper eventMapper;
    private final AuditEventVisibilityPolicy visibilityPolicy;

    @Override
    public AuditSourceResult fetch(AuditQuery query) {
        var logs = activityLogRepository.findByHome_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
                query.homeId(),
                query.from(),
                query.to()
        );

        var configChanges = logs.stream()
                .filter(configLikeActivityDecider::isConfigLike)
                .map(configChangeMapper::map)
                .sorted((a, b) -> compareDesc(a.getCreatedAt(), b.getCreatedAt()))
                .toList();

        var events = logs.stream()
                .filter(log -> !configLikeActivityDecider.isConfigLike(log))
                .map(eventMapper::map)
                .filter(visibilityPolicy::shouldDisplay)
                .sorted((a, b) -> compareDesc(a.getCreatedAt(), b.getCreatedAt()))
                .toList();

        return AuditSourceResult.of(configChanges, events);
    }

    private <T extends Comparable<T>> int compareDesc(T left, T right) {
        if (left == null && right == null) return 0;
        if (left == null) return 1;
        if (right == null) return -1;
        return right.compareTo(left);
    }
}