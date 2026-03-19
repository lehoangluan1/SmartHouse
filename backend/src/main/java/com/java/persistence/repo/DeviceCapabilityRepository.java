package com.java.persistence.repo;

import com.java.persistence.entity.DeviceCapabilityEntity;
import com.java.persistence.entity.DeviceCapabilityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceCapabilityRepository extends JpaRepository<DeviceCapabilityEntity, DeviceCapabilityId> {

    DeviceCapabilityEntity findByDeviceIdAndCapabilityCode(Long deviceId, String capabilityCode);
}