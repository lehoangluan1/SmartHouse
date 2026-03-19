package com.java.controller;

import com.java.config.ApiResponse;
import com.java.controller.dto.AuditDashboardResponse;
import com.java.domain.service.AuditDashboardService;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditQueryController {

    private final AuditDashboardService auditDashboardService;

    @GetMapping("/homes/{homeId}")
    public ApiResponse<AuditDashboardResponse> byHome(
            @PathVariable Long homeId,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(defaultValue = "0") int configPage,
            @RequestParam(defaultValue = "10") int configSize,
            @RequestParam(defaultValue = "") String configKeyword,
            @RequestParam(defaultValue = "0") int eventPage,
            @RequestParam(defaultValue = "10") int eventSize,
            @RequestParam(defaultValue = "") String eventKeyword,
            @RequestParam(defaultValue = "all") String eventCategory
    ) {
        return ApiResponse.ok(
                auditDashboardService.getAuditDashboard(
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
                )
        );
    }
}