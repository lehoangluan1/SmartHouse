package com.java.persistence.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.entity.DeviceRuntimeStateId;

public interface DeviceRuntimeStateRepository
        extends JpaRepository<DeviceRuntimeStateEntity, DeviceRuntimeStateId> {

    @Query("""
        select s
        from DeviceRuntimeStateEntity s
        where s.device.id = :deviceId
    """)
    List<DeviceRuntimeStateEntity> findByIdDeviceId(@Param("deviceId") Long deviceId);

    @Query("""
        select s
        from DeviceRuntimeStateEntity s
        where s.device.id in :deviceIds
    """)
    List<DeviceRuntimeStateEntity> findByIdDeviceIdIn(@Param("deviceIds") List<Long> deviceIds);
}