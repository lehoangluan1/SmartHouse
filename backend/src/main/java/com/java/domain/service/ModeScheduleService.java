package com.java.domain.service;

import com.java.config.NotFoundException;
import com.java.controller.dto.ModeScheduleResponse;
import com.java.controller.dto.ModeScheduleUpsertRequest;
import com.java.mapper.ModeScheduleMapper;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.ScheduleEntity;
import com.java.persistence.repo.ScheduleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModeScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ModeScheduleSupport modeScheduleSupport;
    private final ModeScheduleValidator modeScheduleValidator;
    private final ModeScheduleConflictChecker modeScheduleConflictChecker;
    private final ModeScheduleMapper modeScheduleMapper;
    private final HomeAccessGuard homeAccessGuard;

    public List<ModeScheduleResponse> list(Long homeId) {
        homeAccessGuard.requireHomeMembership(homeId);
        modeScheduleSupport.ensureHomeHasDevice(homeId);

        return scheduleRepository.findByHomeIdOrderByStartTimeAsc(homeId).stream()
                .filter(modeScheduleSupport::isModeSchedule)
                .map(modeScheduleMapper::toResponse)
                .toList();
    }

    @Transactional
    public ModeScheduleResponse create(Long homeId, ModeScheduleUpsertRequest request) {
        homeAccessGuard.requireActivatedProfile(homeId);

        DeviceEntity device = modeScheduleSupport.resolveScheduleDeviceForHome(homeId);

        ScheduleEntity schedule = modeScheduleMapper.toNewModeSchedule(device, request);
        modeScheduleValidator.validateSchedule(schedule);
        modeScheduleConflictChecker.validateConflict(homeId, schedule, null);

        ScheduleEntity saved = scheduleRepository.save(schedule);
        return modeScheduleMapper.toResponse(saved);
    }

    @Transactional
    public ModeScheduleResponse update(Long homeId, Long scheduleId, ModeScheduleUpsertRequest request) {
        homeAccessGuard.requireActivatedProfile(homeId);
        modeScheduleSupport.ensureHomeHasDevice(homeId);

        ScheduleEntity schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Schedule not found with id=" + scheduleId));

        modeScheduleValidator.validateBelongToHomeAndMode(homeId, schedule);
        modeScheduleMapper.merge(schedule, request);

        modeScheduleValidator.validateSchedule(schedule);
        modeScheduleConflictChecker.validateConflict(homeId, schedule, schedule.getId());

        ScheduleEntity saved = scheduleRepository.save(schedule);
        return modeScheduleMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long homeId, Long scheduleId) {
        homeAccessGuard.requireActivatedProfile(homeId);
        modeScheduleSupport.ensureHomeHasDevice(homeId);

        ScheduleEntity schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Schedule not found with id=" + scheduleId));

        modeScheduleValidator.validateBelongToHomeAndMode(homeId, schedule);
        scheduleRepository.delete(schedule);
    }
}