package com.java.domain.service;

import com.java.config.NotFoundException;
import com.java.domain.SystemMode;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.ScheduleEntity;
import com.java.persistence.repo.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModeScheduleSupport {

    public static final String MODE_CAPABILITY = "MODE";

    private final DeviceRepository deviceRepository;

    public DeviceEntity resolveScheduleDeviceForHome(Long homeId) {
        return deviceRepository.findFirstByHome_IdOrderByIdAsc(homeId)
                .orElseThrow(() -> new NotFoundException("No device found in home id=" + homeId));
    }

    public void ensureHomeHasDevice(Long homeId) {
        if (!deviceRepository.existsByHome_Id(homeId)) {
            throw new NotFoundException("No device found in home id=" + homeId);
        }
    }

    public boolean isModeSchedule(ScheduleEntity schedule) {
        return schedule != null
                && schedule.getCapabilityCode() != null
                && MODE_CAPABILITY.equalsIgnoreCase(schedule.getCapabilityCode());
    }

    public String normalizeModeValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return SystemMode.valueOf(raw.trim().toLowerCase()).name();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasModeValue(String value) {
        return normalizeModeValue(value) != null;
    }
    
}