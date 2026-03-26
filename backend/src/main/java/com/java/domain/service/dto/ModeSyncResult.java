
package com.java.domain.service.dto;
public class ModeSyncResult {

    private final Long homeId;
    private final Long firstChangedDeviceId;
    private final String mode;
    private final boolean changed;

    public ModeSyncResult(Long homeId, Long firstChangedDeviceId, String mode, boolean changed) {
        this.homeId = homeId;
        this.firstChangedDeviceId = firstChangedDeviceId;
        this.mode = mode;
        this.changed = changed;
    }

    public Long getHomeId() {
        return homeId;
    }

    public Long getFirstChangedDeviceId() {
        return firstChangedDeviceId;
    }

    public String getMode() {
        return mode;
    }

    public boolean isChanged() {
        return changed;
    }
}