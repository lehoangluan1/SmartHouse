package com.java.persistence.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.persistence.entity.SensorEntity;

public interface SensorRepository extends JpaRepository<SensorEntity, Long> {

    List<SensorEntity> findByDeviceId(Long deviceId);

    Optional<SensorEntity> findByDeviceIdAndSensorKind(Long deviceId, String sensorKind);

    Optional<SensorEntity> findFirstByDeviceHomeIdAndSensorKindOrderByUpdatedAtDesc(Long homeId, String sensorKind);
}