package com.java.domain.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class DashboardSseSessionRegistry {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByHome =
            new ConcurrentHashMap<>();

    public void add(Long homeId, SseEmitter emitter) {
        emittersByHome
                .computeIfAbsent(homeId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);
    }

    public List<SseEmitter> get(Long homeId) {
        return emittersByHome.getOrDefault(homeId, new CopyOnWriteArrayList<>());
    }

    public Map<Long, CopyOnWriteArrayList<SseEmitter>> getAll() {
        return emittersByHome;
    }

    public void remove(Long homeId, SseEmitter emitter) {
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