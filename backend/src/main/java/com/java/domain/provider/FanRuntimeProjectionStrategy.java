package com.java.domain.provider;

import com.java.domain.service.dto.DeviceCapabilities;
import com.java.domain.service.dto.DeviceStateProjection;
import com.java.mapper.DeviceRuntimeProjectionValueMapper;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FanRuntimeProjectionStrategy implements DeviceRuntimeProjectionStrategy {

    private static final Set<String> SUPPORTED = Set.of("FAN", "AIR_CONDITIONER");

    private final DeviceSubtypeResolver subtypeResolver;
    private final DeviceRuntimeStateValueReader valueReader;
    private final DeviceRuntimeProjectionValueMapper valueMapper;

    public FanRuntimeProjectionStrategy(
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
        return SUPPORTED.contains(subtypeResolver.normalize(subtype));
    }

    @Override
    public void apply(Map<String, DeviceRuntimeStateEntity> stateMap, DeviceStateProjection projection) {
        Object powerValue = valueReader.read(stateMap.get(DeviceCapabilities.POWER));
        Object speedValue = valueReader.read(stateMap.get(DeviceCapabilities.SPEED));

        projection.setFanStatus(valueMapper.toOnOff(powerValue));
        projection.setFanSpeed(valueMapper.toInteger(speedValue));
    }
}