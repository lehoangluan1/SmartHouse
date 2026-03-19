package com.java.domain.provider;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.ScheduleEntity;

@Component
public class DefaultScheduleSelectionPolicy implements ScheduleSelectionPolicy {

    private static final int MINUTES_PER_DAY = 24 * 60;

    @Override
    public Map<Long, ScheduleEntity> selectOnePerDevice(List<ScheduleEntity> schedules) {
        Map<Long, ScheduleEntity> result = new LinkedHashMap<>();

        schedules.stream()
                .sorted(schedulePriorityComparator())
                .forEach(schedule -> result.putIfAbsent(schedule.getDevice().getId(), schedule));

        return result;
    }

    private Comparator<ScheduleEntity> schedulePriorityComparator() {
        return Comparator
                .comparing(ScheduleEntity::getPriority, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(this::scheduleDurationMinutes)
                .thenComparing(ScheduleEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private int scheduleDurationMinutes(ScheduleEntity schedule) {
        LocalTime start = schedule.getStartTime();
        LocalTime end = schedule.getEndTime();

        if (start == null || end == null) {
            return Integer.MAX_VALUE;
        }

        if (start.equals(end)) {
            return MINUTES_PER_DAY;
        }

        int startMin = toMinuteOfDay(start);
        int endMin = toMinuteOfDay(end);

        if (endMin > startMin) {
            return endMin - startMin;
        }

        return (MINUTES_PER_DAY - startMin) + endMin;
    }

    private int toMinuteOfDay(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}