package com.java.domain.provider;

import com.java.controller.dto.DeviceCreateRequest;
import com.java.domain.EntityStatus;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceEntityFactory {

    private final DeviceSubtypeResolver deviceSubtypeResolver;

    public DeviceEntity create(DeviceCreateRequest request, HomeEntity home, UserEntity installedBy) {
        String subtype = deviceSubtypeResolver.normalize(request.subtype());

        DeviceEntity device = new DeviceEntity();
        device.setName(request.name().trim());
        device.setDeviceKey(request.deviceKey().trim());
        device.setSubtype(subtype);
        device.setDeviceClass(deviceSubtypeResolver.resolveDeviceClass(subtype));
        device.setRoomName(normalizeNullable(request.roomName()));
        device.setHome(home);
        device.setStatus(EntityStatus.ACTIVE);
        device.setIsOnline(false);
        device.setInstalledBy(installedBy);
        return device;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}