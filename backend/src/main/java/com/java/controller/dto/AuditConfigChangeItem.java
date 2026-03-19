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
public class AuditConfigChangeItem {
    private String source; // DEVICE_CONFIG / ACTIVITY_LOG
    private Long id;

    private Long homeId;
    private Long deviceId;
    private String deviceName;

    private Long userId;
    private String username;

    private String prevConfig;
    private String newConfig;
    private String reason;

    private OffsetDateTime createdAt;
}