package com.java.domain.service;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.ActivityLogResponse;
import com.java.controller.dto.CursorPageResponse;
import com.java.domain.service.dto.AuditLogCursor;
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

    private static final int DEFAULT_CURSOR_LIMIT = 50;
    private static final int MAX_CURSOR_LIMIT = 100;

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

    @Transactional(readOnly = true)
    public CursorPageResponse<ActivityLogResponse> getCursorPage(
            Long homeId,
            Long deviceId,
            Long actorId,
            String action,
            String entityType,
            OffsetDateTime from,
            OffsetDateTime to,
            Integer limit,
            String cursor
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Invalid time: from must be before or equal to to");
        }

        int safeLimit = resolveLimit(limit);
        AuditLogCursor decodedCursor = decodeCursor(cursor);
        String normalizedAction = normalizeNullable(action);
        String normalizedEntityType = normalizeEntityType(entityType);

        List<ActivityLogEntity> rows = activityLogRepository.findCursorPage(
                homeId,
                deviceId,
                actorId,
                normalizedAction,
                normalizedEntityType,
                from,
                to,
                decodedCursor == null ? null : decodedCursor.createdAt(),
                decodedCursor == null ? null : decodedCursor.id(),
                PageRequest.of(0, safeLimit + 1)
        );

        boolean hasMore = rows.size() > safeLimit;
        List<ActivityLogEntity> pageRows = hasMore
                ? new ArrayList<>(rows.subList(0, safeLimit))
                : rows;

        String nextCursor = null;
        if (hasMore && !pageRows.isEmpty()) {
            ActivityLogEntity last = pageRows.get(pageRows.size() - 1);
            nextCursor = encodeCursor(new AuditLogCursor(last.getCreatedAt(), last.getId()));
        }

        return new CursorPageResponse<>(
                pageRows.stream().map(this::toResponse).toList(),
                nextCursor,
                hasMore
        );
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

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_CURSOR_LIMIT;
        }
        if (limit <= 0) {
            throw new BadRequestException("limit must be greater than 0");
        }
        return Math.min(limit, MAX_CURSOR_LIMIT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEntityType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!"DEVICE".equals(normalized) && !"SYSTEM".equals(normalized)) {
            throw new BadRequestException("entityType must be DEVICE or SYSTEM");
        }
        return normalized;
    }

    private AuditLogCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor.trim());
            String json = new String(bytes, StandardCharsets.UTF_8);
            AuditLogCursor decoded = objectMapper.readValue(json, AuditLogCursor.class);
            if (decoded.createdAt() == null || decoded.id() == null) {
                throw new BadRequestException("Invalid cursor");
            }
            return decoded;
        } catch (IllegalArgumentException | JacksonException e) {
            throw new BadRequestException("Invalid cursor");
        }
    }

    private String encodeCursor(AuditLogCursor cursor) {
        try {
            String json = objectMapper.writeValueAsString(cursor);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JacksonException e) {
            throw new BadRequestException("Unable to encode cursor");
        }
    }
}
