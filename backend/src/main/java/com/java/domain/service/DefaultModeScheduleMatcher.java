package com.java.domain.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Component;
import com.java.persistence.entity.ScheduleEntity;

@Component
public class DefaultModeScheduleMatcher implements DeviceScheduleMatcher {

    private static final int MINUTES_PER_DAY = 24 * 60;

    private final ModeScheduleSupport modeScheduleSupport;

    public DefaultModeScheduleMatcher(ModeScheduleSupport modeScheduleSupport) {
        this.modeScheduleSupport = modeScheduleSupport;
    }

    @Override
    public List<ScheduleEntity> findMatchedSchedules(List<ScheduleEntity> schedules, LocalDate date, LocalTime time) {
        int todayBit = dayBit(DayOfWeek.from(date));

        return schedules.stream()
                .filter(this::hasRequiredFields)
                .filter(this::isModeSchedule)
                .filter(schedule -> isDayMatched(schedule, todayBit))
                .filter(schedule -> isTimeMatched(schedule, time))
                .toList();
    }

    private boolean isModeSchedule(ScheduleEntity schedule) {
        return schedule != null
                && modeScheduleSupport.isModeSchedule(schedule)
                && modeScheduleSupport.hasModeValue(schedule.getValueText());
    }

    private boolean hasRequiredFields(ScheduleEntity schedule) {
        return schedule != null
                && Boolean.TRUE.equals(schedule.getEnabled())
                && schedule.getDevice() != null
                && schedule.getDevice().getId() != null
                && schedule.getDevice().getHome() != null
                && schedule.getDevice().getHome().getId() != null
                && schedule.getDaysMask() != null
                && schedule.getStartTime() != null
                && schedule.getEndTime() != null;
    }

    private boolean isDayMatched(ScheduleEntity schedule, int todayBit) {
        return (schedule.getDaysMask() & todayBit) != 0;
    }

    private boolean isTimeMatched(ScheduleEntity schedule, LocalTime now) {
        return isInRange(schedule.getStartTime(), schedule.getEndTime(), now);
    }

    private boolean isInRange(LocalTime start, LocalTime end, LocalTime now) {
        if (start == null || end == null || now == null) {
            return false;
        }

        if (end.equals(start)) {
            return true;
        }

        if (end.isAfter(start)) {
            return !now.isBefore(start) && now.isBefore(end);
        }

        return !now.isBefore(start) || now.isBefore(end);
    }

    @SuppressWarnings("unused")
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

    private int dayBit(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 4;
            case THURSDAY -> 8;
            case FRIDAY -> 16;
            case SATURDAY -> 32;
            case SUNDAY -> 64;
        };
    }
}