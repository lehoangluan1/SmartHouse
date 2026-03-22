package com.java.domain.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardRealtimeService {

    private static final long SSE_TIMEOUT = 0L;

    private final DashboardSseSessionRegistry sessionRegistry;
    private final DashboardRealtimePayloadBuilder payloadBuilder;

    public SseEmitter subscribe(Long homeId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        sessionRegistry.add(homeId, emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE completed for homeId={}", homeId);
            removeEmitter(homeId, emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE timeout for homeId={}", homeId);
            cleanupEmitter(homeId, emitter, null);
        });

        emitter.onError(ex -> {
            log.debug("SSE error for homeId={}: {}", homeId, ex.getMessage());
            cleanupEmitter(homeId, emitter, ex);
        });

        if (!send(homeId, emitter, "CONNECTED", payloadBuilder.connected(homeId))) {
            cleanupEmitter(homeId, emitter, null);
        }

        return emitter;
    }

    public void publish(Long homeId, String eventName, Object payload) {
        if (homeId == null) {
            return;
        }

        List<SseEmitter> emitters = sessionRegistry.get(homeId);
        if (emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            if (!send(homeId, emitter, eventName, payload)) {
                cleanupEmitter(homeId, emitter, null);
            }
        }
    }

    public void publishTelemetryReceived(
            Long homeId,
            Long deviceId,
            Double valueNumeric,
            String valueText,
            Boolean valueBoolean,
            java.time.OffsetDateTime createdAt
    ) {
        if (homeId == null) {
            return;
        }

        publish(
                homeId,
                "TELEMETRY_RECEIVED",
                payloadBuilder.telemetryReceived(
                        homeId,
                        deviceId,
                        valueNumeric,
                        valueText,
                        valueBoolean,
                        createdAt
                )
        );
    }

    public void publishDeviceStateChanged(
            Long homeId,
            Long deviceId,
            String status,
            Integer speed,
            Integer brightness
    ) {
        if (homeId == null) {
            return;
        }

        publish(
                homeId,
                "DEVICE_STATE_CHANGED",
                payloadBuilder.deviceStateChanged(homeId, deviceId, status, speed, brightness)
        );
    }

    public void publishHomeModeChanged(Long homeId, Long deviceId, String mode) {
        if (homeId == null) {
            return;
        }

        publish(
                homeId,
                "HOME_MODE_CHANGED",
                payloadBuilder.homeModeChanged(homeId, deviceId, mode)
        );
    }

    public void heartbeatAll() {
        for (Map.Entry<Long, java.util.concurrent.CopyOnWriteArrayList<SseEmitter>> entry
                : sessionRegistry.getAll().entrySet()) {

            Long homeId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();

            for (SseEmitter emitter : emitters) {
                if (!send(homeId, emitter, "HEARTBEAT", payloadBuilder.heartbeat(homeId))) {
                    cleanupEmitter(homeId, emitter, null);
                }
            }
        }
    }

    private boolean send(Long homeId, SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload));
            return true;
        } catch (IOException | IllegalStateException ex) {
            log.debug(
                    "SSE send failed for homeId={}, eventName={}: {}",
                    homeId,
                    eventName,
                    ex.getMessage()
            );
            return false;
        }
    }

    private void cleanupEmitter(Long homeId, SseEmitter emitter, Throwable ex) {
        removeEmitter(homeId, emitter);

        try {
            if (ex != null) {
                emitter.completeWithError(ex);
            } else {
                emitter.complete();
            }
        } catch (Exception ignore) {
        }
    }

    private void removeEmitter(Long homeId, SseEmitter emitter) {
        sessionRegistry.remove(homeId, emitter);
    }
}