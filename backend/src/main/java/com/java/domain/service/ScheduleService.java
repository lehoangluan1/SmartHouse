package com.java.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.ScheduleResponse;
import com.java.controller.dto.ScheduleUpsertRequest;
import com.java.mapper.ScheduleMapper;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.ScheduleEntity;
import com.java.persistence.repo.DeviceRepository;
import com.java.persistence.repo.ScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DeviceRepository deviceRepository;

    private final ScheduleRequestValidator scheduleRequestValidator;
    private final ScheduleValueResolver scheduleValueResolver;
    private final ScheduleOverlapPolicy scheduleOverlapPolicy;
    private final ScheduleMapper scheduleMapper;
    private final ScheduleActivityLogger scheduleActivityLogger;
    private final HomeAccessGuard homeAccessGuard;

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getByDevice(Long deviceId) {
        DeviceEntity device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BadRequestException("Device not found"));

        if (device.getHome() == null || device.getHome().getId() == null) {
            throw new BadRequestException("Device is not associated with any home");
        }

        homeAccessGuard.requireHomeMembership(device.getHome().getId());

        return scheduleRepository.findByDevice_IdOrderByStartTimeAsc(deviceId)
                .stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Transactional
    public ScheduleResponse upsert(ScheduleUpsertRequest request) {
        scheduleRequestValidator.validate(request);

        DeviceEntity device = deviceRepository.findById(request.deviceId())
                .orElseThrow(() -> new BadRequestException("Device does not exist"));

        if (device.getHome() == null || device.getHome().getId() == null) {
            throw new BadRequestException("Device is not associated with any home");
        }

        Long homeId = device.getHome().getId();
        homeAccessGuard.requireActivatedProfile(homeId);

        String capabilityCode = scheduleValueResolver.resolveCapabilityCode(request);
        ScheduleTypedValue typedValue = scheduleValueResolver.resolveTypedValue(request);

        scheduleOverlapPolicy.validateNoOverlap(request, capabilityCode);

        ScheduleEntity entity = request.id() == null
                ? new ScheduleEntity()
                : scheduleRepository.findById(request.id())
                        .orElseThrow(() -> new BadRequestException("Schedule not found"));

        scheduleMapper.apply(entity, device, capabilityCode, typedValue, request);

        ScheduleEntity saved = scheduleRepository.save(entity);
        scheduleActivityLogger.logUpsert(saved);

        return scheduleMapper.toResponse(saved);
    }
}