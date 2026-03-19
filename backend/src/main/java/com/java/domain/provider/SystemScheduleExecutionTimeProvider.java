package com.java.domain.provider;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

@Component
public class SystemScheduleExecutionTimeProvider implements ScheduleExecutionTimeProvider {

    private final Clock clock;

    public SystemScheduleExecutionTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    @Override
    public LocalTime currentMinute() {
        return LocalTime.now(clock).withSecond(0).withNano(0);
    }
}

