package com.java.domain.provider;

import java.util.List;
import java.util.Map;

import com.java.persistence.entity.ScheduleEntity;

public interface ScheduleSelectionPolicy {
    Map<Long, ScheduleEntity> selectOnePerDevice(List<ScheduleEntity> schedules);
}