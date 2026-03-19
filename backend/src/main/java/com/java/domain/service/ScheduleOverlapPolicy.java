package com.java.domain.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.controller.dto.ScheduleUpsertRequest;
import com.java.persistence.entity.ScheduleEntity;
import com.java.persistence.repo.ScheduleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleOverlapPolicy {

    private static final int DEFAULT_DAYS_MASK = 127;
    private static final int MINUTES_PER_DAY = 24 * 60;

    private final ScheduleRepository scheduleRepository;

    public void validateNoOverlap(ScheduleUpsertRequest request, String capabilityCode) {
        int requestDaysMask = Optional.ofNullable(request).map(ScheduleUpsertRequest::daysMask).orElse( DEFAULT_DAYS_MASK);

        List<ScheduleEntity> currentSchedules = scheduleRepository.findEnabledByDeviceId(request.deviceId());

        for (ScheduleEntity current : currentSchedules) {
            if (request.id() != null && request.id().equals(current.getId())) {
                continue;
            }

            if (current.getCapabilityCode() == null
                    || !current.getCapabilityCode().equalsIgnoreCase(capabilityCode)) {
                continue;
            }

            boolean dayConflict = (current.getDaysMask() & requestDaysMask) != 0;
            boolean timeConflict = overlaps(
                    current.getStartTime(),
                    current.getEndTime(),
                    request.startTime(),
                    request.endTime()
            );

            if (dayConflict && timeConflict) {
                throw new BadRequestException("Schedule overlaps with schedule id = " + current.getId());
            }
        }
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        if (aStart == null || bStart == null) {
            return false;
        }

        if (aEnd == null || bEnd == null) {
            return aStart.equals(bStart);
        }

        if (aStart.equals(aEnd) || bStart.equals(bEnd)) {
            return true;
        }

        boolean aCrossMidnight = aEnd.isBefore(aStart);
        boolean bCrossMidnight = bEnd.isBefore(bStart);

        if (!aCrossMidnight && !bCrossMidnight) {
            return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
        }

        return overlapsExpanded(aStart, aEnd, bStart, bEnd);
    }

    private boolean overlapsExpanded(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        int aStartMin = toMinuteOfDay(aStart);
        int aEndMin = toMinuteOfDay(aEnd);
        int bStartMin = toMinuteOfDay(bStart);
        int bEndMin = toMinuteOfDay(bEnd);

        if (aEndMin <= aStartMin) {
            aEndMin += MINUTES_PER_DAY;
        }
        if (bEndMin <= bStartMin) {
            bEndMin += MINUTES_PER_DAY;
        }

        boolean directOverlap = aStartMin < bEndMin && bStartMin < aEndMin;
        boolean overlapWithShiftA = (aStartMin + MINUTES_PER_DAY) < bEndMin
                && bStartMin < (aEndMin + MINUTES_PER_DAY);
        boolean overlapWithShiftB = aStartMin < (bEndMin + MINUTES_PER_DAY)
                && (bStartMin + MINUTES_PER_DAY) < aEndMin;

        return directOverlap || overlapWithShiftA || overlapWithShiftB;
    }

    private int toMinuteOfDay(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}