package com.java.domain.provider;

import com.java.domain.service.dto.AuditQuery;
import com.java.domain.service.dto.AuditSourceResult;
import com.java.mapper.AlertAuditEventMapper;
import com.java.persistence.repo.AlertRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertAuditProvider implements AuditSourceProvider {

    private final AlertRepository alertRepository;
    private final AlertAuditEventMapper mapper;

    @Override
    public AuditSourceResult fetch(AuditQuery query) {
        var events = alertRepository
                .findByHomeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        query.homeId(),
                        query.from(),
                        query.to()
                )
                .stream()
                .map(mapper::map)
                .toList();

        return AuditSourceResult.of(List.of(), events);
    }
}