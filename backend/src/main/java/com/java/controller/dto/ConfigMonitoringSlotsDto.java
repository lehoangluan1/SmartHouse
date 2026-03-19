package com.java.controller.dto;

public record ConfigMonitoringSlotsDto(
        Long temperatureDeviceId,
        Long humidityDeviceId,
        Long lightSensorDeviceId,
        Long motionDeviceId,
        Long fanDeviceId,
        Long lightDeviceId
) {
}