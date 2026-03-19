package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.ActivityLogResponse;
import com.java.persistence.entity.ActivityLogEntity;
import com.java.persistence.repo.ActivityLogRepository;
import com.java.persistence.repo.DeviceRepository;
import com.java.persistence.repo.HomeRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final HomeRepository homeRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(
            Long homeId,
            Long deviceId,
            Long userId,
            String action,
            String method,
            String oldValue,
            String newValue,
            String detail
    ) {
        log(homeId, deviceId, userId, action, method, (Object) oldValue, (Object) newValue, (Object) detail);
    }

    @Transactional
    public void log(
            Long homeId,
            Long deviceId,
            Long userId,
            String action,
            String method,
            Object oldValue,
            Object newValue,
            Object detail
    ) {
        ActivityLogEntity entity = new ActivityLogEntity();

        if (homeId != null) {
            entity.setHome(homeRepository.getReferenceById(homeId));
        }
        if (deviceId != null) {
            entity.setDevice(deviceRepository.getReferenceById(deviceId));
        }
        if (userId != null) {
            entity.setUser(userRepository.getReferenceById(userId));
        }

        entity.setAction(action);
        entity.setMethod(method);
        entity.setOldValue(toJson(oldValue));
        entity.setNewValue(toJson(newValue));
        entity.setDetail(toJson(detail));

        activityLogRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getByHome(Long homeId, OffsetDateTime from, OffsetDateTime to) {
        if (homeId == null) {
            throw new BadRequestException("homeId cannot be empty");
        }
        if (from == null || to == null) {
            throw new BadRequestException("from/to cannot be empty");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("Invalid time: from must be before or equal to to");
        }

        return activityLogRepository.findByHome_IdAndCreatedAtBetweenOrderByCreatedAtDesc(homeId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ActivityLogResponse toResponse(ActivityLogEntity entity) {
        return new ActivityLogResponse(
                entity.getId(),
                entity.getHome() != null ? entity.getHome().getId() : null,
                entity.getDevice() != null ? entity.getDevice().getId() : null,
                entity.getDevice() != null ? entity.getDevice().getName() : null,
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getUser() != null ? entity.getUser().getUsername() : null,
                entity.getAction(),
                entity.getMethod(),
                entity.getOldValue(),
                entity.getNewValue(),
                entity.getDetail(),
                entity.getCreatedAt()
        );
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new BadRequestException("Unable to serialize activity log");
        }
    }
}