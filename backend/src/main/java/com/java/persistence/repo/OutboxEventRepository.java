package com.java.persistence.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.domain.OutboxStatus;
import com.java.persistence.entity.OutboxEventEntity;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}