package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.domain.SystemMode;
import com.java.domain.service.dto.ManualHoldState;
import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.ConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManualHoldService {

    private static final String ACTION_STARTED = "MANUAL_HOLD_STARTED";
    private static final String ACTION_RESTORED = "MANUAL_HOLD_RESTORED";
    private static final String ACTION_CLEARED = "MANUAL_HOLD_CLEARED";

    private static final int DEFAULT_THOLD_MINUTES = 5;

    private final ConfigRepository configRepository;
    private final ActivityLogService activityLogService;
    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final ModeAutomationService modeAutomationService;
    private final ManualHoldQueryService manualHoldQueryService;

    @Transactional
    public void enableManualHold(DeviceEntity device, SystemMode previousMode, Long actorId, String actorName) {
        if (device == null || device.getId() == null || device.getHome() == null || device.getHome().getId() == null) {
            return;
        }

        Long homeId = device.getHome().getId();

        ConfigEntity cfg = configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(homeId).orElse(null);
        int holdMinutes = resolveHoldMinutes(cfg);

        OffsetDateTime holdUntil = OffsetDateTime.now().plusMinutes(holdMinutes);

        activityLogService.log(
                homeId,
                device.getId(),
                actorId,
                ACTION_STARTED,
                "SYSTEM",
                null,
                null,
                Map.of(
                        "deviceId", device.getId(),
                        "homeId", homeId,
                        "previousMode", previousMode.name(),
                        "holdUntil", holdUntil.toString(),
                        "holdMinutes", holdMinutes,
                        "actorName", actorName
                )
        );
    }

    @Transactional
    public boolean restoreIfExpired(Long deviceId) {
        var opt = manualHoldQueryService.getCurrentHold(deviceId);
        if (opt.isEmpty()) {
            return false;
        }

        ManualHoldState state = opt.get();
        if (OffsetDateTime.now().isBefore(state.holdUntil())) {
            return false;
        }

        deviceRuntimeStateService.syncModeForHome(
                state.homeId(),
                state.previousMode().name(),
                "MANUAL_HOLD_RESTORE",
                null,
                null
        );

        activityLogService.log(
                state.homeId(),
                state.deviceId(),
                null,
                ACTION_RESTORED,
                "SYSTEM",
                null,
                null,
                Map.of(
                        "deviceId", state.deviceId(),
                        "homeId", state.homeId(),
                        "restoredMode", state.previousMode().name(),
                        "expiredAt", state.holdUntil().toString()
                )
        );

        modeAutomationService.evaluateAllByHome(state.homeId());
        return true;
    }

    @Transactional
    public void clearHold(Long deviceId, Long actorId, String actorName, String reason) {
        var opt = manualHoldQueryService.getCurrentHold(deviceId);
        if (opt.isEmpty()) {
            return;
        }

        ManualHoldState state = opt.get();

        activityLogService.log(
                state.homeId(),
                state.deviceId(),
                actorId,
                ACTION_CLEARED,
                "MANUAL",
                null,
                null,
                Map.of(
                        "deviceId", state.deviceId(),
                        "homeId", state.homeId(),
                        "previousMode", state.previousMode().name(),
                        "holdUntil", state.holdUntil().toString(),
                        "reason", reason == null ? "" : reason,
                        "actorName", actorName == null ? "" : actorName
                )
        );
    }

    private int resolveHoldMinutes(ConfigEntity cfg) {
        if (cfg == null || cfg.getTholdMinutes() == null || cfg.getTholdMinutes() <= 0) {
            return DEFAULT_THOLD_MINUTES;
        }
        return cfg.getTholdMinutes();
    }
}