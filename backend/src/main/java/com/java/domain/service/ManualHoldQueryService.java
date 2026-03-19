package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.domain.SystemMode;
import com.java.domain.service.dto.ManualHoldState;
import com.java.persistence.entity.ActivityLogEntity;
import com.java.persistence.repo.ActivityLogRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ManualHoldQueryService {

    private static final String ACTION_STARTED = "MANUAL_HOLD_STARTED";
    private static final String ACTION_RESTORED = "MANUAL_HOLD_RESTORED";
    private static final String ACTION_CLEARED = "MANUAL_HOLD_CLEARED";

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public boolean isHolding(Long deviceId) {
        return getCurrentHold(deviceId)
                .map(ManualHoldState::active)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean shouldRestore(Long deviceId) {
        return getCurrentHold(deviceId)
                .map(state -> !OffsetDateTime.now().isBefore(state.holdUntil()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<ManualHoldState> getCurrentHold(Long deviceId) {
        List<ActivityLogEntity> logs = activityLogRepository.findLatestHoldLogs(
                deviceId,
                List.of(ACTION_STARTED, ACTION_RESTORED, ACTION_CLEARED)
        );

        if (logs == null || logs.isEmpty()) {
            return Optional.empty();
        }

        ActivityLogEntity latest = logs.get(0);
        if (!ACTION_STARTED.equalsIgnoreCase(latest.getAction())) {
            return Optional.empty();
        }

        Map<String, Object> detail = convertDetail(latest.getDetail());
        if (detail.isEmpty()) {
            return Optional.empty();
        }

        try {
            Long homeId = asLong(detail.get("homeId"));
            String previousModeText = String.valueOf(detail.get("previousMode"));
            String holdUntilText = String.valueOf(detail.get("holdUntil"));

            SystemMode previousMode = SystemMode.valueOf(previousModeText.toLowerCase());
            OffsetDateTime holdUntil = OffsetDateTime.parse(holdUntilText);

            boolean active = OffsetDateTime.now().isBefore(holdUntil);

            return Optional.of(new ManualHoldState(
                    deviceId,
                    homeId,
                    previousMode,
                    holdUntil,
                    active
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Map<String, Object> convertDetail(Object raw) {
        if (raw == null) {
            return Map.of();
        }

        if (raw instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) map;
            return result;
        }

        if (raw instanceof String text) {
            try {
                return objectMapper.readValue(
                        text,
                        new TypeReference<Map<String, Object>>() {}
                );
            } catch (JacksonException e) {
                return Map.of();
            }
        }

        return objectMapper.convertValue(
                raw,
                new TypeReference<Map<String, Object>>() {}
        );
    }
    
    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return  Long.valueOf(value.toString()) ;
    }
}