package com.java.controller;

import com.java.controller.dto.ActivityLogResponse;
import com.java.controller.dto.CursorPageResponse;
import com.java.domain.service.ActivityLogService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public CursorPageResponse<ActivityLogResponse> list(
            @RequestParam(required = false) Long homeId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestParam(required = false) String cursor
    ) {
        return activityLogService.getCursorPage(
                homeId,
                deviceId,
                actorId,
                action,
                entityType,
                from,
                to,
                limit,
                cursor
        );
    }
}
