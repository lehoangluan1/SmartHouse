package com.java.domain.service.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.java.persistence.entity.ScheduleEntity;

public record ScheduleExecutionContext(
        ScheduleEntity schedule,
        LocalDate executionDate,
        LocalTime executionTime
) {
}