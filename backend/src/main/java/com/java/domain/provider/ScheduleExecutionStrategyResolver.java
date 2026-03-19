package com.java.domain.provider;

import java.util.List;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.ScheduleEntity;

@Component
public class ScheduleExecutionStrategyResolver {

    private final List<ScheduleExecutionStrategy> strategies;

    public ScheduleExecutionStrategyResolver(List<ScheduleExecutionStrategy> strategies) {
        this.strategies = strategies;
    }

    public ScheduleExecutionStrategy resolve(ScheduleEntity schedule) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(schedule))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No matching ScheduleExecutionStrategy found for schedule id="
                                + (schedule != null ? schedule.getId() : null)
                ));
    }
}