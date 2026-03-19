package com.java.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.java.config.ApiResponse;
import com.java.controller.dto.AlertActionRequest;
import com.java.controller.dto.AlertOpenRequest;
import com.java.domain.service.AlertService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/homes/{homeId}/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;

    @GetMapping
    public ApiResponse<?> list(@PathVariable Long homeId) {
        return ApiResponse.ok(alertService.getByHome(homeId));
    }

    @PostMapping
    public ApiResponse<?> open(
            @PathVariable Long homeId,
            @Valid @RequestBody AlertOpenRequest request
    ) {
        return ApiResponse.ok(
                alertService.openOrRefresh(
                        homeId,
                        request.deviceId(),
                        request.sensorId(),
                        request.type(),
                        request.message()
                )
        );
    }

    @PostMapping("/{alertId}/ack")
    public ApiResponse<?> ack(@PathVariable Long alertId, @Valid @RequestBody AlertActionRequest request) {
        return ApiResponse.ok(alertService.acknowledge(alertId, request.userId()));
    }

    @PostMapping("/{alertId}/resolve")
    public ApiResponse<?> resolve(@PathVariable Long alertId, @Valid @RequestBody AlertActionRequest request) {
        return ApiResponse.ok(alertService.resolve(alertId, request.userId()));
    }
}