package com.java.domain.service;


import com.java.controller.dto.AuditConfigChangeItem;
import com.java.controller.dto.AuditDashboardResponse;
import com.java.controller.dto.AuditEventItem;
import com.java.controller.dto.PageResponse;
import com.java.domain.service.dto.AuditQuery;
import com.java.mapper.ActivityAuditEventMapper;
import com.java.mapper.ActivityConfigChangeMapper;
import com.java.persistence.repo.ActivityLogRepository;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditDashboardService {

    private final AuditQueryValidator validator;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityConfigChangeMapper configChangeMapper;
    private final ActivityAuditEventMapper eventMapper;

    @Transactional(readOnly = true)
    public AuditDashboardResponse getAuditDashboard(
            Long homeId,
            OffsetDateTime from,
            OffsetDateTime to,
            int configPage,
            int configSize,
            String configKeyword,
            int eventPage,
            int eventSize,
            String eventKeyword,
            String eventCategory
    ) {
        AuditQuery query = new AuditQuery(
                homeId,
                from,
                to,
                configPage,
                configSize,
                configKeyword,
                eventPage,
                eventSize,
                eventKeyword,
                eventCategory
        );

        validator.validate(query);

        String normalizedConfigKeyword = normalizeKeyword(query.configKeyword());
        String normalizedEventKeyword = normalizeKeyword(query.eventKeyword());
        String normalizedEventCategory = normalizeCategory(query.eventCategory());

        var configPageRows = activityLogRepository.findAuditConfigChanges(
                query.homeId(),
                query.from(),
                query.to(),
                normalizedConfigKeyword,
                PageRequest.of(query.configPage(), query.configSize())
        );

        var eventPageRows = activityLogRepository.findAuditEvents(
                query.homeId(),
                query.from(),
                query.to(),
                normalizedEventCategory,
                normalizedEventKeyword,
                PageRequest.of(query.eventPage(), query.eventSize())
        );

        PageResponse<AuditConfigChangeItem> configPageResponse = PageResponse.of(
                configPageRows.getContent().stream().map(configChangeMapper::map).toList(),
                query.configPage(),
                query.configSize(),
                configPageRows.getTotalElements()
        );

        PageResponse<AuditEventItem> eventPageResponse = PageResponse.of(
                eventPageRows.getContent().stream().map(eventMapper::map).toList(),
                query.eventPage(),
                query.eventSize(),
                eventPageRows.getTotalElements()
        );

        return AuditDashboardResponse.builder()
                .summary(buildSummary(
                        query,
                        normalizedEventCategory,
                        normalizedEventKeyword,
                        configPageRows.getTotalElements()
                ))
                .configChanges(configPageResponse)
                .events(eventPageResponse)
                .build();
    }

    private com.java.controller.dto.AuditSummaryResponse buildSummary(
            AuditQuery query,
            String selectedCategory,
            String eventKeyword,
            long configCount
    ) {
        long alerts = countCategory(query, selectedCategory, eventKeyword, "alerts");
        long device = countCategory(query, selectedCategory, eventKeyword, "device");
        long system = countCategory(query, selectedCategory, eventKeyword, "system");
        long total = alerts + device + system;

        return com.java.controller.dto.AuditSummaryResponse.builder()
                .alerts(alerts)
                .device(device)
                .system(system)
                .totalEvents(total)
                .configChanges(configCount)
                .build();
    }

    private long countCategory(AuditQuery query, String selectedCategory, String eventKeyword, String category) {
        if (!"all".equals(selectedCategory) && !selectedCategory.equals(category)) {
            return 0L;
        }

        return activityLogRepository.countAuditEvents(
                query.homeId(),
                query.from(),
                query.to(),
                category,
                eventKeyword
        );
    }

    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCategory(String value) {
        if (value == null || value.isBlank()) {
            return "all";
        }
        return value.trim().toLowerCase();
    }
}
