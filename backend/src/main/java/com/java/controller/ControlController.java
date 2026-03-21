package com.java.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.java.config.ApiResponse;
import com.java.controller.dto.ControlRequest;
import com.java.controller.dto.ManualControlExecutionRequest;
import com.java.domain.service.ControlFacadeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/control")
@RequiredArgsConstructor
public class ControlController {

    private final ControlFacadeService controlFacadeService;

    @PostMapping("/devices/{deviceId}")
    public ApiResponse<?> control(
            @PathVariable Long deviceId,
            @Valid @RequestBody ControlRequest request
    ) {
        return ApiResponse.ok(
                controlFacadeService.control(
                        new ManualControlExecutionRequest(deviceId, request)
                )
        );
    }
}