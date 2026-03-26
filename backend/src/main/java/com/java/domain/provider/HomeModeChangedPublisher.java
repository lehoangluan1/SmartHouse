package com.java.domain.provider;

public interface HomeModeChangedPublisher {
    void publish(Long homeId, Long deviceId, String mode);
}