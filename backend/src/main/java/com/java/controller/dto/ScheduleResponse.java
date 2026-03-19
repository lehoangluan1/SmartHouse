package com.java.controller.dto;

import java.time.LocalTime;
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
public class ScheduleResponse {
    private Long id;
    private Long deviceId;
    private String capabilityCode;
    private String value;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer daysMask;
    private Boolean enabled;
    private Integer priority;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}