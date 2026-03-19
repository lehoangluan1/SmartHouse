package com.java.domain.service;


import com.java.domain.provider.AuditSourceProvider;
import com.java.domain.service.dto.AuditQuery;
import com.java.domain.service.dto.AuditSourceResult;
import com.java.mapper.CommandAuditEventMapper;
import com.java.persistence.repo.ControlCommandRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandAuditProvider implements AuditSourceProvider {

    private final ControlCommandRepository controlCommandRepository;
    private final CommandAuditEventMapper mapper;

    @Override
    public AuditSourceResult fetch(AuditQuery query) {
        var events = controlCommandRepository
                .findByDeviceHomeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
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