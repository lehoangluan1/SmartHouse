package com.java.eventing;

public interface DomainEventListener<T extends DomainEvent> {
    boolean supports(DomainEvent event);
    void onEvent(T event);
}