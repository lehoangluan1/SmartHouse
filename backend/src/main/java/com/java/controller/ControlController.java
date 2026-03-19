package com.java.controller;

import com.java.config.ApiResponse;
import com.java.controller.dto.ControlRequest;
import com.java.domain.service.ControlFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/control")
@RequiredArgsConstructor
public class ControlController {
    private final ControlFacadeService controlFacadeService;

    @PostMapping("/devices/{deviceId}")
    public ApiResponse<?> control(@PathVariable Long deviceId, @Valid @RequestBody ControlRequest request) {
        return ApiResponse.ok(controlFacadeService.manualControl(deviceId, request));
    }
}
