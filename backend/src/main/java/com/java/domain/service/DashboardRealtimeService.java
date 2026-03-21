package com.java.domain.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DashboardRealtimeService {

    private static final long SSE_TIMEOUT = 0L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByHome =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long homeId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emittersByHome
                .computeIfAbsent(homeId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(homeId, emitter));
        emitter.onTimeout(() -> removeEmitter(homeId, emitter));
        emitter.onError(ex -> removeEmitter(homeId, emitter));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "CONNECTED");
        payload.put("homeId", homeId);

        Map<String, Object> innerPayload = new LinkedHashMap<>();
        innerPayload.put("connectedAt", OffsetDateTime.now().toString());

        payload.put("payload", innerPayload);

        send(emitter, "CONNECTED", payload);
        return emitter;
    }

    public void publish(Long homeId, String eventName, Object payload) {
        if (homeId == null) {
            return;
        }

        List<SseEmitter> emitters = emittersByHome.get(homeId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                send(emitter, eventName, payload);
            } catch (Exception ex) {
                log.debug("Remove broken SSE emitter for home {}", homeId);
                removeEmitter(homeId, emitter);
            }
        }
    }

    public void publishTelemetryReceived(
            Long homeId,
            Long deviceId,
            Double valueNumeric,
            String valueText,
            Boolean valueBoolean,
            OffsetDateTime createdAt
    ) {
        if (homeId == null) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "TELEMETRY_RECEIVED");
        payload.put("homeId", homeId);
        payload.put("deviceId", deviceId);

        Map<String, Object> innerPayload = new LinkedHashMap<>();
        innerPayload.put("valueNumeric", valueNumeric);
        innerPayload.put("valueText", valueText);
        innerPayload.put("valueBoolean", valueBoolean);
        innerPayload.put(
                "createdAt",
                createdAt == null ? OffsetDateTime.now().toString() : createdAt.toString()
        );

        payload.put("payload", innerPayload);

        publish(homeId, "TELEMETRY_RECEIVED", payload);
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

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "DEVICE_STATE_CHANGED");
        payload.put("homeId", homeId);
        payload.put("deviceId", deviceId);

        Map<String, Object> innerPayload = new LinkedHashMap<>();
        innerPayload.put("status", status);
        innerPayload.put("speed", speed);
        innerPayload.put("brightness", brightness);

        payload.put("payload", innerPayload);

        publish(homeId, "DEVICE_STATE_CHANGED", payload);
    }

    public void publishHomeModeChanged(Long homeId, Long deviceId, String mode) {
        if (homeId == null) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "HOME_MODE_CHANGED");
        payload.put("homeId", homeId);
        payload.put("deviceId", deviceId);

        Map<String, Object> innerPayload = new LinkedHashMap<>();
        innerPayload.put("mode", mode);

        payload.put("payload", innerPayload);

        publish(homeId, "HOME_MODE_CHANGED", payload);
    }

    public void heartbeatAll() {
        emittersByHome.forEach((homeId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("type", "HEARTBEAT");
                    payload.put("homeId", homeId);

                    Map<String, Object> innerPayload = new LinkedHashMap<>();
                    innerPayload.put("ts", OffsetDateTime.now().toString());

                    payload.put("payload", innerPayload);

                    send(emitter, "HEARTBEAT", payload);
                } catch (Exception ex) {
                    removeEmitter(homeId, emitter);
                }
            }
        });
    }

    private void send(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send SSE event", ex);
        }
    }

    private void removeEmitter(Long homeId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByHome.get(homeId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);

        if (emitters.isEmpty()) {
            emittersByHome.remove(homeId);
        }
    }
}