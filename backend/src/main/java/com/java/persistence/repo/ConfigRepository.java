package com.java.persistence.repo;

import com.java.persistence.entity.ConfigEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfigRepository extends JpaRepository<ConfigEntity, Long> {

    List<ConfigEntity> findByHomeIdOrderByUpdatedAtDesc(Long homeId);

    Optional<ConfigEntity> findFirstByHomeIdOrderByUpdatedAtDesc(Long homeId);

    Optional<ConfigEntity> findFirstByHomeIdAndIsActiveTrue(Long homeId);

    @Modifying
    @Query("""
        update ConfigEntity c
        set c.isActive = false
        where c.home.id = :homeId
          and c.isActive = true
    """)
    int deactivateAllByHomeId(@Param("homeId") Long homeId);
}