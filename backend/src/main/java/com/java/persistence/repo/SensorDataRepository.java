package com.java.persistence.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.java.persistence.entity.SensorDataEntity;

public interface SensorDataRepository extends JpaRepository<SensorDataEntity, Long> {

    Optional<SensorDataEntity> findFirstBySensor_IdOrderByCreatedAtDesc(Long sensorId);

    List<SensorDataEntity> findBySensor_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long sensorId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<SensorDataEntity> findBySensor_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            Long sensorId,
            OffsetDateTime since
    );

    @Query("""
        select t
        from SensorDataEntity t
        where t.sensor.device.id = :deviceId
          and t.createdAt >= :since
        order by t.createdAt asc
    """)
    List<SensorDataEntity> findSince(
            @Param("deviceId") Long deviceId,
            @Param("since") OffsetDateTime since
    );

    @Query("""
        select t
        from SensorDataEntity t
        where t.sensor.device.id = :deviceId
        order by t.createdAt desc
    """)
    List<SensorDataEntity> findLatestList(@Param("deviceId") Long deviceId);

    default SensorDataEntity findLatest(Long deviceId) {
        List<SensorDataEntity> list = findLatestList(deviceId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Query("""
        select t
        from SensorDataEntity t
        where t.sensor.device.id = :deviceId
          and t.createdAt between :from and :to
        order by t.createdAt asc
    """)
    List<SensorDataEntity> findRange(
            @Param("deviceId") Long deviceId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    List<SensorDataEntity> findBySensor_IdOrderByCreatedAtDesc(Long sensorId);

    default SensorDataEntity findLatestBySensorId(Long sensorId) {
        List<SensorDataEntity> list = findBySensor_IdOrderByCreatedAtDesc(sensorId);
        return list.isEmpty() ? null : list.get(0);
    }
}