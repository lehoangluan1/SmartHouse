package com.java.domain.service;

import com.java.domain.DeviceClass;
import com.java.domain.provider.DeviceRuntimeProjectionStrategy;
import com.java.domain.provider.DeviceSubtypeResolver;
import com.java.domain.service.dto.DeviceStateProjection;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DeviceSubtypePolicy {

    private final DeviceSubtypeResolver subtypeResolver;
    private final List<DeviceRuntimeProjectionStrategy> projectionStrategies;

    public DeviceSubtypePolicy(
            DeviceSubtypeResolver subtypeResolver,
            List<DeviceRuntimeProjectionStrategy> projectionStrategies
    ) {
        this.subtypeResolver = subtypeResolver;
        this.projectionStrategies = projectionStrategies;
    }

    public String normalizeSubtype(String subtype) {
        return subtypeResolver.normalize(subtype);
    }

    public DeviceClass resolveDeviceClass(String subtype) {
        return subtypeResolver.resolveDeviceClass(subtype);
    }

    public boolean isSupportedMonitoringSubtype(String subtype) {
        return subtypeResolver.isSupportedMonitoringSubtype(subtype);
    }

    public void applyState(
            String subtype,
            Map<String, DeviceRuntimeStateEntity> stateMap,
            DeviceStateProjection projection
    ) {
        projectionStrategies.stream()
                .filter(strategy -> strategy.supports(subtype))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Projection strategy not found"))
                .apply(stateMap, projection);
    }
}