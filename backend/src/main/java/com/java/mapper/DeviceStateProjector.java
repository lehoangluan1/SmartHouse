package com.java.mapper;

import com.java.controller.dto.DeviceResponse;
import com.java.controller.dto.DeviceStateResponse;
import com.java.domain.service.dto.DeviceStateProjection;

public final class DeviceStateProjector {

    private DeviceStateProjector() {
    }

    public static DeviceStateProjection forDeviceResponse(DeviceResponse.DeviceResponseBuilder builder) {
        return new DeviceStateProjection() {
            @Override
            public void setFanStatus(String value) {
                builder.fanStatus(value);
            }

            @Override
            public void setFanSpeed(Integer value) {
                builder.fanSpeed(value);
            }

            @Override
            public void setLightStatus(String value) {
                builder.lightStatus(value);
            }

            @Override
            public void setLightLevel(Integer value) {
                builder.lightLevel(value);
            }
        };
    }

    public static DeviceStateProjection forDeviceStateResponse(DeviceStateResponse.DeviceStateResponseBuilder builder) {
        return new DeviceStateProjection() {
            @Override
            public void setFanStatus(String value) {
                builder.fanStatus(value);
            }

            @Override
            public void setFanSpeed(Integer value) {
                builder.fanSpeed(value);
            }

            @Override
            public void setLightStatus(String value) {
                builder.lightStatus(value);
            }

            @Override
            public void setLightLevel(Integer value) {
                builder.lightLevel(value);
            }
        };
    }
}