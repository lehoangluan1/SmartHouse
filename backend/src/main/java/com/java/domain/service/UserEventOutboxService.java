package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.java.domain.OutboxStatus;
import com.java.persistence.entity.OutboxEventEntity;
import com.java.persistence.repo.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class UserEventOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void enqueue(String eventType, Long aggregateId, Object payload) {
        try {
            OutboxEventEntity event = new OutboxEventEntity();
            event.setAggregateType("HOME_USER");
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus(OutboxStatus.PENDING);
            event.setRetryCount(0);
            event.setCreatedAt(OffsetDateTime.now());
            
            outboxEventRepository.save(event);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Cannot save outbox event", ex);
        }
    }
}