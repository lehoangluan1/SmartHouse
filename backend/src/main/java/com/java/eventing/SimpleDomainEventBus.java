package com.java.eventing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SimpleDomainEventBus implements DomainEventBus {

    private final List<DomainEventListener<?>> listeners;

    public SimpleDomainEventBus(List<DomainEventListener<?>> listeners) {
        this.listeners = new CopyOnWriteArrayList<>(listeners);
    }

    @Override
    public void register(DomainEventListener<?> listener) {
        listeners.add(listener);
    }

    @Override
    public void unregister(DomainEventListener<?> listener) {
        listeners.remove(listener);
    }

    @Override
    public void publish(DomainEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(event);
                }
            });
            return;
        }

        doPublish(event);
    }

    private void doPublish(DomainEvent event) {
        for (DomainEventListener<?> listener : listeners) {
            dispatch(listener, event);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> void dispatch(DomainEventListener<?> listener, DomainEvent event) {
        DomainEventListener<T> typedListener = (DomainEventListener<T>) listener;
        if (typedListener.supports(event)) {
            typedListener.onEvent((T) event);
        }
    }
}