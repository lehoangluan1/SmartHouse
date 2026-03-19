package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.OutboxPublisherProperties;
import com.java.config.RabbitUserEventProperties;
import com.java.domain.OutboxStatus;
import com.java.persistence.entity.OutboxEventEntity;
import com.java.persistence.repo.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitUserEventProperties rabbitUserEventProperties;
    private final OutboxPublisherProperties outboxPublisherProperties;

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> events = loadPendingEvents();

        for (OutboxEventEntity event : events) {
            try {
                rabbitTemplate.convertAndSend(
                        rabbitUserEventProperties.exchange(),
                        rabbitUserEventProperties.routingKey(),
                        event.getPayload()
                );
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(OffsetDateTime.now());
                event.setLastError(null);
            } catch (AmqpException ex) {
                event.setStatus(OutboxStatus.FAILED);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(ex.getMessage());
            }
        }
    }

    private List<OutboxEventEntity> loadPendingEvents() {
        Integer batchSize = outboxPublisherProperties.batchSize();
        if (batchSize != null && batchSize <= 50) {
            return outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        }
        return outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }
}