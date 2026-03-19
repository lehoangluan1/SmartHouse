package com.java.controller.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventItem {
    private String source;   // ALERT / COMMAND / ACTIVITY_LOG
    private Long id;

    private String category; // alerts / device / system
    private String type;
    private String status;

    private Long homeId;
    private Long deviceId;
    private String deviceName;

    private Long userId;
    private String username;

    private String method;
    private String fromState;
    private String toState;
    private String details;

    private OffsetDateTime createdAt;
}