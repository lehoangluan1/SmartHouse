package com.java.eventing;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.java.domain.provider.HomeModeChangedPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpringHomeModeChangedPublisher implements HomeModeChangedPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(Long homeId, Long deviceId, String mode) {
        applicationEventPublisher.publishEvent(
                HomeModeChangedEvent.builder()
                        .homeId(homeId)
                        .deviceId(deviceId)
                        .mode(mode)
                        .build()
        );
    }
}