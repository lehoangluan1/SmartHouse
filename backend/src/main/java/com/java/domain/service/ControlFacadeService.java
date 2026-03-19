package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.controller.dto.ControlCommandResponse;
import com.java.controller.dto.ControlRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ControlFacadeService {

    private final ManualControlService manualControlService;
    private final AutoControlService autoControlService;

    @Transactional
    public ControlCommandResponse manualControl(Long deviceId, ControlRequest request) {
        return manualControlService.execute(deviceId, request);
    }

    @Transactional
    public void autoControl(Long deviceId, String target, String value, String method) {
        autoControlService.autoControl(deviceId, target, value, method);
    }
}