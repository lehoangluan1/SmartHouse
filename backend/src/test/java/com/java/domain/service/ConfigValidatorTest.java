package com.java.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.java.config.BadRequestException;
import com.java.controller.dto.ConfigMonitoringSlotsDto;
import com.java.controller.dto.ConfigThresholdsDto;
import com.java.controller.dto.ConfigUpsertRequest;
import com.java.domain.DeviceClass;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.repo.DeviceRepository;

class ConfigValidatorTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final ConfigValidator validator = new ConfigValidator(deviceRepository);

    @Test
    void acceptsLightSensorNodeAndLightActuatorInSeparateSlots() {
        DeviceEntity temp = device(1L, "ohstem-temp-01", DeviceClass.SENSOR_NODE, "TEMPERATURE_NODE");
        DeviceEntity humidity = device(2L, "ohstem-humidity-01", DeviceClass.SENSOR_NODE, "HUMIDITY_NODE");
        DeviceEntity lightSensor = device(3L, "ohstem-light-01", DeviceClass.SENSOR_NODE, "LIGHT_NODE");
        DeviceEntity motion = device(4L, "ohstem-motion-01", DeviceClass.SENSOR_NODE, "MOTION_NODE");
        DeviceEntity fan = device(5L, "ohstem-fan-ctrl-01", DeviceClass.ACTUATOR, "FAN");
        DeviceEntity light = device(6L, "ohstem-light-ctrl-01", DeviceClass.ACTUATOR, "LIGHT");
        mockDevices(temp, humidity, lightSensor, motion, fan, light);

        assertThatCode(() -> validator.validate(1L, request(1L, 2L, 3L, 4L, 5L, 6L)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsLightActuatorInLightSensorSlot() {
        DeviceEntity lightActuator = device(6L, "ohstem-light-ctrl-01", DeviceClass.ACTUATOR, "LIGHT");
        mockDevices(lightActuator);

        assertThatThrownBy(() -> validator.validate(1L, request(null, null, 6L, null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Light sensor")
                .hasMessageContaining("ohstem-light-ctrl-01")
                .hasMessageContaining("SENSOR_NODE/LIGHT_NODE");
    }

    @Test
    void rejectsLightSensorInLightActuatorSlot() {
        DeviceEntity lightSensor = device(3L, "ohstem-light-01", DeviceClass.SENSOR_NODE, "LIGHT_NODE");
        mockDevices(lightSensor);

        assertThatThrownBy(() -> validator.validate(1L, request(null, null, null, null, null, 3L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Light actuator")
                .hasMessageContaining("ohstem-light-01")
                .hasMessageContaining("ACTUATOR/LIGHT");
    }

    private ConfigUpsertRequest request(
            Long temperatureId,
            Long humidityId,
            Long lightSensorId,
            Long motionId,
            Long fanId,
            Long lightId
    ) {
        return new ConfigUpsertRequest(
                "Default",
                new ConfigThresholdsDto(30.0, 27.0, 35, 55, 29.0, 26.0, 32.0, 35.0, 2, 2, 5, 3, 10, 70, 40, 55),
                new ConfigMonitoringSlotsDto(temperatureId, humidityId, lightSensorId, motionId, fanId, lightId)
        );
    }

    private DeviceEntity device(Long id, String key, DeviceClass deviceClass, String subtype) {
        HomeEntity home = new HomeEntity();
        home.setId(1L);

        DeviceEntity device = new DeviceEntity();
        device.setId(id);
        device.setHome(home);
        device.setDeviceKey(key);
        device.setName(key);
        device.setDeviceClass(deviceClass);
        device.setSubtype(subtype);
        return device;
    }

    private void mockDevices(DeviceEntity... devices) {
        for (DeviceEntity device : devices) {
            when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        }
    }
}
