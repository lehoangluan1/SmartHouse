package com.java.domain.service;


import com.java.controller.dto.AuditConfigChangeItem;
import com.java.controller.dto.AuditDashboardResponse;
import com.java.controller.dto.AuditEventItem;
import com.java.controller.dto.PageResponse;
import com.java.domain.provider.AuditSourceProvider;
import com.java.domain.service.dto.AuditQuery;
import com.java.domain.service.dto.AuditSourceResult;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditDashboardService {

    private final AuditQueryValidator validator;
    private final List<AuditSourceProvider> providers;
    private final AuditFilterService filterService;
    private final AuditSummaryAssembler summaryAssembler;
    private final PaginationService paginationService;

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

        List<AuditConfigChangeItem> allConfigChanges = new ArrayList<>();
        List<AuditEventItem> allEvents = new ArrayList<>();

        for (AuditSourceProvider provider : providers) {
            AuditSourceResult result = provider.fetch(query);
            allConfigChanges.addAll(result.configChanges());
            allEvents.addAll(result.events());
        }

        allConfigChanges = allConfigChanges.stream()
                .sorted(Comparator.comparing(
                        AuditConfigChangeItem::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        allEvents = allEvents.stream()
                .sorted(Comparator.comparing(
                        AuditEventItem::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        List<AuditConfigChangeItem> filteredConfigChanges =
                filterService.filterConfigChanges(allConfigChanges, query.configKeyword());

        List<AuditEventItem> filteredEvents =
                filterService.filterEvents(allEvents, query.eventKeyword(), query.eventCategory());

        PageResponse<AuditConfigChangeItem> configPageResponse =
                paginationService.paginate(filteredConfigChanges, query.configPage(), query.configSize());

        PageResponse<AuditEventItem> eventPageResponse =
                paginationService.paginate(filteredEvents, query.eventPage(), query.eventSize());

        return AuditDashboardResponse.builder()
                .summary(summaryAssembler.assemble(filteredConfigChanges, filteredEvents))
                .configChanges(configPageResponse)
                .events(eventPageResponse)
                .build();
    }
}