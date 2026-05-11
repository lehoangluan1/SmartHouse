package com.java.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.java.config.BadRequestException;
import com.java.config.InvalidTelemetryException;
import com.java.controller.dto.TelemetryIngestRequest;
import com.java.domain.events.TelemetryReceivedEvent;
import com.java.domain.service.dto.TelemetryPersistenceResult;
import com.java.eventing.DomainEventBus;
import com.java.mapper.TelemetryEventMapper;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryIngestService {

    private final TelemetryPersistenceService telemetryPersistenceService;
    private final TelemetryEventMapper telemetryEventMapper;
    private final DomainEventBus eventBus;
    private final InvalidTelemetryHandler invalidTelemetryHandler;
    private final DeviceRepository deviceRepository;
    private final PlatformTransactionManager transactionManager;
    private final TelemetryAutomationService telemetryAutomationService;

    @Value("${app.telemetry.async.enabled:false}")
    private boolean asyncEnabled;

    @Value("${app.telemetry.async.queue-capacity:1000}")
    private int asyncQueueCapacity;

    @Value("${app.telemetry.async.batch-size:50}")
    private int asyncBatchSize;

    private BlockingQueue<TelemetryIngestRequest> queue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    @PostConstruct
    void startWorkerIfNeeded() {
        if (!asyncEnabled) {
            return;
        }
        queue = new ArrayBlockingQueue<>(Math.max(1, asyncQueueCapacity));
        running.set(true);
        worker = new Thread(this::workerLoop, "telemetry-ingest-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("Telemetry async ingest enabled queueCapacity={} batchSize={}", asyncQueueCapacity, asyncBatchSize);
    }

    @PreDestroy
    void stopWorker() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
        }
    }

    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    public boolean enqueue(TelemetryIngestRequest request) {
        if (!asyncEnabled) {
            ingest(request);
            return true;
        }
        boolean accepted = queue.offer(request);
        if (!accepted) {
            log.warn("Telemetry async queue full size={} deviceKey={}", queue.size(), request.deviceKey());
        }
        return accepted;
    }

    @Transactional
    public void ingest(TelemetryIngestRequest request) {
        persistAndPublish(request);
    }

    private void persistAndPublish(TelemetryIngestRequest request) {
        try {
            var result = telemetryPersistenceService.persist(request);

            eventBus.publish(telemetryEventMapper.toEvent(result, request.value()));
            eventBus.publish(new TelemetryReceivedEvent(
                    result.device().getHome().getId(),
                    result.device().getId(),
                    result.sensor().getId(),
                    result.sensorType().name(),
                    request.value()
            ));
            scheduleAutomationAfterCommit(result);

        } catch (InvalidTelemetryException ex) {
            deviceRepository.findByDeviceKey(request.deviceKey()).ifPresent(device ->
                    invalidTelemetryHandler.handle(device, request.sensorType(), request.value(), ex.getMessage())
            );
            throw new BadRequestException(ex.getMessage());
        }
    }

    private void scheduleAutomationAfterCommit(TelemetryPersistenceResult result) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    telemetryAutomationService.handle(result);
                }
            });
            return;
        }

        telemetryAutomationService.handle(result);
    }

    private void workerLoop() {
        List<TelemetryIngestRequest> batch = new ArrayList<>();
        while (running.get()) {
            try {
                TelemetryIngestRequest first = queue.poll(250, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }

                batch.clear();
                batch.add(first);
                queue.drainTo(batch, Math.max(0, asyncBatchSize - 1));

                for (TelemetryIngestRequest request : batch) {
                    try {
                        new TransactionTemplate(transactionManager)
                                .executeWithoutResult(status -> persistAndPublish(request));
                    } catch (RuntimeException ex) {
                        log.warn(
                                "Telemetry async persist failed deviceKey={} sensorType={} err={}",
                                request.deviceKey(),
                                request.sensorType(),
                                ex.getMessage()
                        );
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException ex) {
                log.warn("Telemetry async worker error err={}", ex.getClass().getSimpleName());
            }
        }
    }
}
