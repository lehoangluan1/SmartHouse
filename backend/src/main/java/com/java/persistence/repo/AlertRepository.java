package com.java.persistence.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.domain.AlertStatus;
import com.java.domain.AlertType;
import com.java.persistence.entity.AlertEntity;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    List<AlertEntity> findByHomeIdAndStatusOrderByCreatedAtDesc(Long homeId, AlertStatus status);

    List<AlertEntity> findByHomeIdOrderByCreatedAtDesc(Long homeId);

    List<AlertEntity> findByHomeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long homeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<AlertEntity> findByHomeIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long homeId,
            AlertStatus status,
            OffsetDateTime from,
            OffsetDateTime to
    );

    Optional<AlertEntity> findFirstByDeviceIdAndTypeAndStatusOrderByCreatedAtDesc(
            Long deviceId,
            AlertType type,
            AlertStatus status
    );

    Optional<AlertEntity> findFirstByDeviceIdAndSensorIdAndTypeAndStatusOrderByCreatedAtDesc(
            Long deviceId,
            Long sensorId,
            AlertType type,
            AlertStatus status
    );

    Optional<AlertEntity> findFirstByDeviceIdAndSensorIdIsNullAndTypeAndStatusOrderByCreatedAtDesc(
            Long deviceId,
            AlertType type,
            AlertStatus status
    );

    default AlertEntity findTopOpen(Long deviceId, AlertType type) {
        return findFirstByDeviceIdAndSensorIdIsNullAndTypeAndStatusOrderByCreatedAtDesc(
                deviceId,
                type,
                AlertStatus.ACTIVE
        ).orElse(null);
    }

    default AlertEntity findTopOpen(Long deviceId, Long sensorId, AlertType type) {
        if (sensorId == null) {
            return findFirstByDeviceIdAndSensorIdIsNullAndTypeAndStatusOrderByCreatedAtDesc(
                    deviceId,
                    type,
                    AlertStatus.ACTIVE
            ).orElse(null);
        }

        return findFirstByDeviceIdAndSensorIdAndTypeAndStatusOrderByCreatedAtDesc(
                deviceId,
                sensorId,
                type,
                AlertStatus.ACTIVE
        ).orElse(null);
    }
}