package com.java.domain.provider;

import com.java.domain.service.dto.DeviceCapabilities;
import com.java.domain.service.dto.DeviceStateProjection;
import com.java.mapper.DeviceRuntimeProjectionValueMapper;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LightRuntimeProjectionStrategy implements DeviceRuntimeProjectionStrategy {

    private final DeviceSubtypeResolver subtypeResolver;
    private final DeviceRuntimeStateValueReader valueReader;
    private final DeviceRuntimeProjectionValueMapper valueMapper;

    public LightRuntimeProjectionStrategy(
            DeviceSubtypeResolver subtypeResolver,
            DeviceRuntimeStateValueReader valueReader,
            DeviceRuntimeProjectionValueMapper valueMapper
    ) {
        this.subtypeResolver = subtypeResolver;
        this.valueReader = valueReader;
        this.valueMapper = valueMapper;
    }

    @Override
    public boolean supports(String subtype) {
        return "LIGHT".equals(subtypeResolver.normalize(subtype));
    }

    @Override
    public void apply(Map<String, DeviceRuntimeStateEntity> stateMap, DeviceStateProjection projection) {
        Object powerValue = valueReader.read(stateMap.get(DeviceCapabilities.POWER));
        Object brightnessValue = valueReader.read(stateMap.get(DeviceCapabilities.BRIGHTNESS));

        projection.setLightStatus(valueMapper.toOnOff(powerValue));
        projection.setLightLevel(valueMapper.toInteger(brightnessValue));
    }
}