package com.java.domain.provider;
import java.time.LocalDate;
import java.time.LocalTime;

public interface ScheduleExecutionTimeProvider {
    LocalDate currentDate();
    LocalTime currentMinute();
}