package com.java.persistence.repo;

import com.java.persistence.entity.ActivityLogEntity;
import java.time.OffsetDateTime;
import java.util.List;

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
}