package com.java.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.java.eventing.DomainEventBus;
import com.java.eventing.DomainEventListener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class EventBusWiringConfig {

    private final DomainEventBus bus;
    private final List<DomainEventListener<?>> listeners;

    @PostConstruct
    public void wire() {
        for (DomainEventListener<?> listener : listeners) {
            bus.register(listener);
        }
    }
}