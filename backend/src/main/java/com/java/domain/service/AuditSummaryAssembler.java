package com.java.domain.service;

import com.java.controller.dto.AuditConfigChangeItem;
import com.java.controller.dto.AuditEventItem;
import com.java.controller.dto.AuditSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuditSummaryAssembler {

    public AuditSummaryResponse assemble(
            List<AuditConfigChangeItem> configChanges,
            List<AuditEventItem> events
    ) {
        long alertCount = events.stream()
                .filter(e -> "alerts".equals(e.getCategory()))
                .count();

        long deviceCount = events.stream()
                .filter(e -> "device".equals(e.getCategory()))
                .count();

        long systemCount = events.stream()
                .filter(e -> "system".equals(e.getCategory()))
                .count();

        return AuditSummaryResponse.builder()
                .alerts(alertCount)
                .device(deviceCount)
                .system(systemCount)
                .totalEvents(events.size())
                .configChanges(configChanges.size())
                .build();
    }
}
