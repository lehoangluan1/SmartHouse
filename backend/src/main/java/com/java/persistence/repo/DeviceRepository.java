package com.java.persistence.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.domain.DeviceClass;
import com.java.persistence.entity.DeviceEntity;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    Optional<DeviceEntity> findByDeviceKey(String deviceKey);

    List<DeviceEntity> findByDeviceKeyIn(List<String> deviceKeys);

    List<DeviceEntity> findByHomeId(Long homeId);

    List<DeviceEntity> findByLastSeenBefore(OffsetDateTime threshold);

    Optional<DeviceEntity> findFirstByHomeIdAndDeviceClass(Long homeId, DeviceClass deviceClass);
    
    Optional<DeviceEntity> findFirstByHome_IdOrderByIdAsc(Long homeId);

    boolean existsByHome_Id(Long homeId);
}
