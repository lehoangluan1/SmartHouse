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
public class DeviceStateResponse {
    private Long deviceId;
    private String fanStatus;
    private Integer fanSpeed;
    private String lightStatus;
    private Integer lightLevel;
    private String mode;
    private String prevMode;
    private OffsetDateTime holdUntil;
    private OffsetDateTime updatedAt;
}