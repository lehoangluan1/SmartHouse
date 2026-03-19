package com.java.persistence.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.java.persistence.entity.ScheduleEntity;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

    List<ScheduleEntity> findByDevice_IdAndEnabledTrue(Long deviceId);

    List<ScheduleEntity> findByEnabledTrue();

    List<ScheduleEntity> findByDevice_IdOrderByStartTimeAsc(Long deviceId);

    @Query("""
        select s
        from ScheduleEntity s
        where s.device.id = :deviceId
          and s.enabled = true
        order by s.startTime asc
    """)
    List<ScheduleEntity> findEnabledByDeviceId(@Param("deviceId") Long deviceId);

    @Query("""
        select s
        from ScheduleEntity s
        where s.device.home.id = :homeId
        order by s.startTime asc
    """)
    List<ScheduleEntity> findByHomeIdOrderByStartTimeAsc(@Param("homeId") Long homeId);

    @Query("""
        select s
        from ScheduleEntity s
        where s.device.home.id = :homeId
          and s.enabled = true
        order by s.startTime asc
    """)
    List<ScheduleEntity> findEnabledByHomeId(@Param("homeId") Long homeId);
}