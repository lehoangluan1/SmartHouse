package com.java.persistence.repo;

import com.java.persistence.entity.ActivityLogEntity;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, Long> {

    List<ActivityLogEntity> findByHome_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long homeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<ActivityLogEntity> findByDevice_IdOrderByCreatedAtDesc(Long deviceId);

    @Query("""
        select a
        from ActivityLogEntity a
        where a.device.id = :deviceId
          and a.action in :actions
        order by a.createdAt desc, a.id desc
    """)
    List<ActivityLogEntity> findLatestHoldLogs(
            @Param("deviceId") Long deviceId,
            @Param("actions") List<String> actions
    );

    @Query("""
        select a
        from ActivityLogEntity a
        where a.action = :action
          and a.device is not null
        order by a.createdAt desc, a.id desc
    """)
    List<ActivityLogEntity> findByActionOrderByCreatedAtDesc(@Param("action") String action);

    @Query("""
        select a
        from ActivityLogEntity a
        where (:homeId is null or a.home.id = :homeId)
          and (:deviceId is null or a.device.id = :deviceId)
          and (:actorId is null or a.user.id = :actorId)
          and (:action is null or lower(a.action) = lower(:action))
          and (:entityType is null
               or (:entityType = 'DEVICE' and a.device is not null)
               or (:entityType = 'SYSTEM' and a.device is null))
          and (:from is null or a.createdAt >= :from)
          and (:to is null or a.createdAt <= :to)
          and (:cursorCreatedAt is null
               or a.createdAt < :cursorCreatedAt
               or (a.createdAt = :cursorCreatedAt and a.id < :cursorId))
        order by a.createdAt desc, a.id desc
    """)
    List<ActivityLogEntity> findCursorPage(
            @Param("homeId") Long homeId,
            @Param("deviceId") Long deviceId,
            @Param("actorId") Long actorId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
