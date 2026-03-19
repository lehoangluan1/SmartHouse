package com.java.domain.provider;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.repo.DeviceRuntimeStateRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceRuntimeStateProvider {

    private final DeviceRuntimeStateRepository runtimeStateRepository;

    public Map<String, DeviceRuntimeStateEntity> getRuntimeStateMap(Long deviceId) {
        return runtimeStateRepository.findByIdDeviceId(deviceId).stream()
                .collect(Collectors.toMap(
                        DeviceRuntimeStateEntity::getCapabilityCode,
                        Function.identity(),
                        (a, b) -> b
                ));
    }
}