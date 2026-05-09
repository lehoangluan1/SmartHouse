package com.java.domain.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class CommandLongPollNotifier {

    private static final int MAX_WAITERS_PER_DEVICE = 64;

    private final ConcurrentMap<String, WaitSlot> waitSlots = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "command-long-poll-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    });

    public CompletableFuture<Void> await(String deviceKey, long waitMs) {
        if (deviceKey == null || waitMs <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        return awaitAny(List.of(deviceKey), waitMs);
    }

    public CompletableFuture<Void> awaitAny(Collection<String> deviceKeys, long waitMs) {
        if (deviceKeys == null || deviceKeys.isEmpty() || waitMs <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        List<String> keys = deviceKeys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(key -> !key.isBlank())
                .distinct()
                .toList();
        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        List<WaitSlot> registeredSlots = new ArrayList<>();

        for (String key : keys) {
            WaitSlot slot = waitSlots.computeIfAbsent(key, ignored -> new WaitSlot());
            synchronized (slot) {
                if (slot.waiters.size() >= MAX_WAITERS_PER_DEVICE) {
                    continue;
                }
                slot.waiters.add(future);
                registeredSlots.add(slot);
            }
        }

        if (registeredSlots.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        ScheduledFuture<?> timeoutTask = scheduler.schedule(
                () -> future.complete(null),
                waitMs,
                TimeUnit.MILLISECONDS
        );

        future.whenComplete((ignored, ex) -> {
            timeoutTask.cancel(false);
            for (String key : keys) {
                cleanupWaiter(key, future);
            }
        });

        return future;
    }

    public void awaitBlocking(String deviceKey, long waitMs) {
        await(deviceKey, waitMs).join();
    }

    public int signal(String deviceKey) {
        if (deviceKey == null) {
            return 0;
        }

        WaitSlot slot = waitSlots.get(deviceKey);
        if (slot == null) {
            return 0;
        }

        synchronized (slot) {
            int waiters = slot.waiters.size();
            List<CompletableFuture<Void>> futures = new ArrayList<>(slot.waiters);
            slot.waiters.clear();
            for (CompletableFuture<Void> future : futures) {
                future.complete(null);
            }
            waitSlots.remove(deviceKey, slot);
            return waiters;
        }
    }

    private void cleanupWaiter(String deviceKey, CompletableFuture<Void> future) {
        WaitSlot slot = waitSlots.get(deviceKey);
        if (slot == null) {
            return;
        }

        synchronized (slot) {
            slot.waiters.remove(future);
            if (slot.waiters.isEmpty()) {
                waitSlots.remove(deviceKey, slot);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private static class WaitSlot {
        private final List<CompletableFuture<Void>> waiters = new ArrayList<>();
    }
}
