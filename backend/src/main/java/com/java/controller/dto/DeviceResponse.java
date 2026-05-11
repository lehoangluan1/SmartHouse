package com.java.controller.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {
    private Long id;
    private String name;
    private String deviceKey;
    private String deviceClass;
    private String type;
    private String subtype;
    private String status;
    private Long homeId;
    private String roomName;
    private Boolean online;
    private OffsetDateTime lastSeen;

    private String mode;
    private String fanStatus;
    private Integer fanSpeed;
    private String lightStatus;
    private Integer lightLevel;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
