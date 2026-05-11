package com.java.domain.service;

public interface ModeAutomationService {
    void evaluateAndApply(Long deviceId);
    void evaluateAndApply(Long deviceId, String reason);
    void evaluateAllByHome(Long homeId);
    void evaluateAllByHome(Long homeId, String reason);
}
