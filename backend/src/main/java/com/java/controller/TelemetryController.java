package com.java.controller;

import com.java.config.ApiResponse;
import com.java.controller.dto.TelemetryIngestRequest;
import com.java.domain.service.TelemetryIngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-telemetry")
@RequiredArgsConstructor
public class TelemetryController {
    private final TelemetryIngestService telemetryIngestService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> ingest(@Valid @RequestBody TelemetryIngestRequest request) {
        if (telemetryIngestService.isAsyncEnabled()) {
            boolean queued = telemetryIngestService.enqueue(request);
            if (!queued) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ApiResponse<>(false, null, "Telemetry queue is full"));
            }
            return ResponseEntity.accepted()
                    .body(ApiResponse.ok(null, "Telemetry queued"));
        }

        telemetryIngestService.ingest(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Telemetry accepted"));
    }
}
