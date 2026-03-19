package com.java.persistence.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.persistence.entity.DeviceConfigEntity;

public interface DeviceConfigRepository extends JpaRepository<DeviceConfigEntity, Long> {

    Optional<DeviceConfigEntity> findFirstByDeviceIdOrderByChangedAtDesc(Long deviceId);

    List<DeviceConfigEntity> findByDeviceHomeIdAndChangedAtBetweenOrderByChangedAtDesc(
            Long homeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    default DeviceConfigEntity findLatestByDeviceId(Long deviceId) {
        return findFirstByDeviceIdOrderByChangedAtDesc(deviceId).orElse(null);
    }
}