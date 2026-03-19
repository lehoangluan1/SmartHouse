package com.java.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.java.config.ApiResponse;
import com.java.domain.service.TelemetryHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class TelemetryHistoryController {

    private final TelemetryHistoryService telemetryHistoryService;

    @GetMapping("/{deviceKey}/telemetry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> history(
            @PathVariable String deviceKey,
            @RequestParam(defaultValue = "24h") String range
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                telemetryHistoryService.getHistory(deviceKey, range)
        ));
    }
}