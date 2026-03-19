package com.java.eventing;

public interface DomainEventBus {
    void register(DomainEventListener<?> listener);
    void unregister(DomainEventListener<?> listener);
    void publish(DomainEvent event);
}