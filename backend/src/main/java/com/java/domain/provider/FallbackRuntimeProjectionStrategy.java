package com.java.domain.provider;

import com.java.domain.service.dto.DeviceCapabilities;
import com.java.domain.service.dto.DeviceStateProjection;
import com.java.persistence.entity.DeviceRuntimeStateEntity;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.java.mapper.DeviceRuntimeProjectionValueMapper;

@Component
public class FallbackRuntimeProjectionStrategy implements DeviceRuntimeProjectionStrategy {

    private final DeviceRuntimeStateValueReader valueReader;
    private final DeviceRuntimeProjectionValueMapper valueMapper;

    public FallbackRuntimeProjectionStrategy(DeviceRuntimeStateValueReader valueReader, DeviceRuntimeProjectionValueMapper valueMapper) {
        this.valueReader = valueReader;
        this.valueMapper = valueMapper;
    }

    @Override
    public boolean supports(String subtype) {
        return true;
    }

    @Override
    public void apply(Map<String, DeviceRuntimeStateEntity> stateMap, DeviceStateProjection projection) {
        Object powerValue = valueReader.read(stateMap.get(DeviceCapabilities.POWER));

        if (stateMap.containsKey(DeviceCapabilities.SPEED)) {
            projection.setFanStatus(valueMapper.toOnOff(powerValue));
            projection.setFanSpeed(valueMapper.toInteger( valueReader.read(stateMap.get(DeviceCapabilities.SPEED))));
        }

        if (stateMap.containsKey(DeviceCapabilities.BRIGHTNESS)) {
            projection.setLightStatus(valueMapper.toOnOff(powerValue));
            projection.setLightLevel(valueMapper.toInteger( valueReader.read(stateMap.get(DeviceCapabilities.BRIGHTNESS))));
        }
    }
}