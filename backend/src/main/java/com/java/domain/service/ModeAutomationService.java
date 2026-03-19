package com.java.domain.service;

public interface ModeAutomationService {
    void evaluateAndApply(Long deviceId);
    void evaluateAllByHome(Long homeId);
}
