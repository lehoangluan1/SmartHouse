package com.java.controller.dto;

public record ModeScheduleUpsertRequest(
    String mode,        // auto/manual/sleep/away
    String startTime,   // "23:00"
    String endTime,     // "06:00"
    Integer daysMask,   // 0..127
    Boolean enabled
) {}