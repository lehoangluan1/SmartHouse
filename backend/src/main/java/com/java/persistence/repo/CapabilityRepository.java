package com.java.persistence.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.persistence.entity.CapabilityEntity;
import com.java.persistence.entity.CapabilityId;

public interface CapabilityRepository extends JpaRepository<CapabilityEntity, CapabilityId> {

    boolean existsByDevice_IdAndId_CapabilityCode(Long deviceId, String capabilityCode);

    Optional<CapabilityEntity> findByDevice_IdAndId_CapabilityCode(Long deviceId, String capabilityCode);

    List<CapabilityEntity> findByDevice_Id(Long deviceId);
}