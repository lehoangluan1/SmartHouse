package com.java.domain.provider;
import com.java.domain.service.dto.ScheduleExecutionContext;
import com.java.persistence.entity.ScheduleEntity;

public interface ScheduleExecutionStrategy {
    boolean supports(ScheduleEntity schedule);
    void execute(ScheduleExecutionContext context);
}