package com.java.scheduler;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.java.domain.provider.ScheduleExecutionStrategy;
import com.java.domain.provider.ScheduleExecutionStrategyResolver;
import com.java.domain.provider.ScheduleExecutionTimeProvider;
import com.java.domain.provider.ScheduleSelectionPolicy;
import com.java.domain.service.DeviceScheduleMatcher;
import com.java.domain.service.dto.ScheduleExecutionContext;
import com.java.persistence.entity.ScheduleEntity;
import com.java.persistence.repo.ScheduleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeviceScheduleScheduler {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleExecutionTimeProvider timeProvider;
    private final DeviceScheduleMatcher deviceScheduleMatcher;
    private final ScheduleSelectionPolicy scheduleSelectionPolicy;
    private final ScheduleExecutionStrategyResolver strategyResolver;

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void applySchedules() {
        LocalDate currentDate = timeProvider.currentDate();
        LocalTime currentTime = timeProvider.currentMinute();

        List<ScheduleEntity> enabledSchedules = scheduleRepository.findByEnabledTrue();
        List<ScheduleEntity> matchedSchedules = deviceScheduleMatcher.findMatchedSchedules(
                enabledSchedules,
                currentDate,
                currentTime
        );

        if (matchedSchedules.isEmpty()) {
            return;
        }

        Map<Long, ScheduleEntity> selectedSchedules =
                scheduleSelectionPolicy.selectOnePerDevice(matchedSchedules);

        selectedSchedules.values().forEach(schedule -> {
            ScheduleExecutionStrategy strategy = strategyResolver.resolve(schedule);
            strategy.execute(new ScheduleExecutionContext(schedule, currentDate, currentTime));
        });
    }
}