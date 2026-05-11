package com.java.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.java.config.BadRequestException;
import com.java.domain.DeviceClass;
import com.java.domain.provider.ControllerTargetSupportPolicy;
import com.java.domain.provider.DefaultDeviceSubtypeResolver;
import com.java.domain.provider.DefaultDeviceTargetResolver;
import com.java.domain.provider.FanTargetSupportPolicy;
import com.java.domain.provider.LightTargetSupportPolicy;
import com.java.persistence.entity.DeviceEntity;

class DeviceTargetPolicyTest {

    private DeviceTargetPolicy policy;

    @BeforeEach
    void setUp() {
        DefaultDeviceSubtypeResolver subtypeResolver = new DefaultDeviceSubtypeResolver();
        policy = new DeviceTargetPolicy(
                new DefaultDeviceTargetResolver(),
                subtypeResolver,
                List.of(),
                List.of(
                        new FanTargetSupportPolicy(subtypeResolver),
                        new LightTargetSupportPolicy(subtypeResolver),
                        new ControllerTargetSupportPolicy(subtypeResolver)
                )
        );
    }

    @Test
    void lightActuatorAcceptsLightPowerAndBrightnessTargets() {
        DeviceEntity light = device(DeviceClass.ACTUATOR, "LIGHT");

        assertThatCode(() -> policy.validateTargetForDevice(light, "light"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validateTargetForDevice(light, "brightness"))
                .doesNotThrowAnyException();
    }

    @Test
    void lightSensorCannotBeControlledAsLightActuator() {
        DeviceEntity lightSensor = device(DeviceClass.SENSOR_NODE, "LIGHT_NODE");

        assertThatThrownBy(() -> policy.validateTargetForDevice(lightSensor, "light"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sensor-only");
    }

    private DeviceEntity device(DeviceClass deviceClass, String subtype) {
        DeviceEntity device = new DeviceEntity();
        device.setId(6L);
        device.setDeviceClass(deviceClass);
        device.setSubtype(subtype);
        return device;
    }
}
