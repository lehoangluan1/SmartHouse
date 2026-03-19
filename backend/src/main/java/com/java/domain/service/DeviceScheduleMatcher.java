package com.java.domain.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.java.persistence.entity.ScheduleEntity;

public interface DeviceScheduleMatcher {
    List<ScheduleEntity> findMatchedSchedules(List<ScheduleEntity> schedules, LocalDate date, LocalTime time);
}