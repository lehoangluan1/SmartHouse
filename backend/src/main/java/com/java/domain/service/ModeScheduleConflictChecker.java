package com.java.domain.service;

import com.java.config.BadRequestException;
import com.java.persistence.entity.ScheduleEntity;
import com.java.persistence.repo.ScheduleRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModeScheduleConflictChecker {

    private final ScheduleRepository scheduleRepository;
    private final ModeScheduleSupport modeScheduleSupport;

    public void validateConflict(Long homeId, ScheduleEntity candidate, Long ignoreId) {
        List<ScheduleEntity> schedules = scheduleRepository.findByHomeIdOrderByStartTimeAsc(homeId);

        for (ScheduleEntity existing : schedules) {
            if (!modeScheduleSupport.isModeSchedule(existing)) {
                continue;
            }

            if (ignoreId != null && ignoreId.equals(existing.getId())) {
                continue;
            }

            if (Boolean.FALSE.equals(existing.getEnabled()) || Boolean.FALSE.equals(candidate.getEnabled())) {
                continue;
            }

            if (!hasDayOverlap(existing, candidate)) {
                continue;
            }

            if (timeOverlap(
                    existing.getStartTime(),
                    existing.getEndTime(),
                    candidate.getStartTime(),
                    candidate.getEndTime()
            )) {
                throw new BadRequestException(
                        "Mode schedule time overlaps with schedule id=" + existing.getId()
                );
            }
        }
    }

    private boolean hasDayOverlap(ScheduleEntity existing, ScheduleEntity candidate) {
        int existingMask = Optional.ofNullable(existing.getDaysMask()).orElse(0);
        int candidateMask = Optional.ofNullable(candidate.getDaysMask()).orElse(0);
        return (existingMask & candidateMask) != 0;
    }

    /**
     * endTime == null => treated as an infinite schedule starting from startTime.
     */
    private boolean timeOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        LocalTime resolvedAEnd = aEnd != null ? aEnd : LocalTime.MAX;
        LocalTime resolvedBEnd = bEnd != null ? bEnd : LocalTime.MAX;

        return aStart.isBefore(resolvedBEnd) && bStart.isBefore(resolvedAEnd);
    }
}