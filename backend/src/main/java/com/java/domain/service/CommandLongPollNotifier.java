package com.java.domain.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class CommandLongPollNotifier {

    private final ConcurrentMap<String, WaitSlot> waitSlots = new ConcurrentHashMap<>();

    public void await(String deviceKey, long waitMs) {
        if (deviceKey == null || waitMs <= 0) {
            return;
        }

        WaitSlot slot = waitSlots.computeIfAbsent(deviceKey, key -> new WaitSlot());
        synchronized (slot) {
            slot.waiters++;
            try {
                slot.wait(waitMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                slot.waiters--;
            }
        }
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
            int waiters = slot.waiters;
            slot.notifyAll();
            return waiters;
        }
    }

    private static class WaitSlot {
        private int waiters;
    }
}
