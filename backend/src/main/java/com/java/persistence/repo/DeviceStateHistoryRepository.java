package com.java.persistence.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.java.persistence.entity.DeviceStateHistoryEntity;
import com.java.persistence.repo.projection.DeviceStateHistoryView;

public interface DeviceStateHistoryRepository extends JpaRepository<DeviceStateHistoryEntity, Long> {

    List<DeviceStateHistoryEntity> findByCapability_Device_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long deviceId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<DeviceStateHistoryEntity> findByCapability_Device_IdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> deviceIds,
            OffsetDateTime from,
            OffsetDateTime to
    );

    DeviceStateHistoryEntity findFirstByCapability_Device_IdAndCapability_CapabilityCodeAndCreatedAtBeforeOrderByCreatedAtDesc(
            Long deviceId,
            String capabilityCode,
            OffsetDateTime createdAt
    );

    DeviceStateHistoryEntity findFirstByCapability_Device_IdAndCapability_CapabilityCodeAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
            Long deviceId,
            String capabilityCode,
            OffsetDateTime createdAt
    );

    DeviceStateHistoryEntity findFirstByCapability_Device_IdAndCapability_CapabilityCodeOrderByCreatedAtDesc(
            Long deviceId,
            String capabilityCode
    );

    List<DeviceStateHistoryEntity> findBySourceAndSourceRefIdOrderByCreatedAtAsc(
            String source,
            Long sourceRefId
    );

    @Query("""
        select
                h.id as id,
                h.capabilityCode as capabilityCode,
                h.valueBoolean as valueBoolean,
                h.valueNumber as valueNumber,
                h.valueText as valueText,
                h.createdAt as createdAt
        from DeviceStateHistoryEntity h
        where h.deviceId = :deviceId
          and h.createdAt between :from and :to
        order by h.createdAt asc
        """)
    List<DeviceStateHistoryView> findRange(
            @Param("deviceId") Long deviceId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
        select max(h.createdAt)
        from DeviceStateHistoryEntity h
        where h.deviceId = :deviceId
          and upper(h.capabilityCode) = upper(:target)
          and upper(h.source) = 'AUTO_CONTROL'
        """)
    Optional<OffsetDateTime> findLastAutomationAt(
            @Param("deviceId") Long deviceId,
            @Param("target") String target
    );

    @Query("""
        select max(h.createdAt)
        from DeviceStateHistoryEntity h
        where h.deviceId = :deviceId
          and upper(h.capabilityCode) = upper(:target)
          and upper(h.source) = 'AUTO_CONTROL'
          and (
                (upper(:value) = 'TRUE' and h.valueBoolean = true)
             or (upper(:value) = 'FALSE' and h.valueBoolean = false)
             or (h.valueNumber is not null and str(h.valueNumber) = :value)
             or (h.valueText is not null and upper(h.valueText) = upper(:value))
          )
        """)
    Optional<OffsetDateTime> findLastAutomationAt(
            @Param("deviceId") Long deviceId,
            @Param("target") String target,
            @Param("value") String value
    );
}