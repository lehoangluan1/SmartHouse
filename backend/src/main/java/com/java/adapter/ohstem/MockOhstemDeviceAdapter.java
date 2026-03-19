package com.java.adapter.ohstem;

import org.springframework.stereotype.Component;

import com.java.adapter.DeviceCommandAdapter;
import com.java.persistence.entity.ControlCommandEntity;

@Component
public class MockOhstemDeviceAdapter implements DeviceCommandAdapter {
    @Override
    public boolean supports(String deviceKey) {
        if (deviceKey == null || deviceKey.isBlank()) {
            return false;
        }

        String key = deviceKey.trim().toLowerCase();

        return key.startsWith("hub")
                || key.startsWith("light")
                || key.startsWith("sensor")
                || key.startsWith("fan")
                || key.startsWith("yolobit")
                || key.startsWith("door")
                || key.startsWith("room")
                || key.startsWith("ohstem");
    }

    @Override
    public AdapterResult send(ControlCommandEntity command) {
        return new AdapterResult(
                true,
                "SENT",
                "Mock adapter sent command to device " + command.getDevice().getDeviceKey()
        );
    }
}