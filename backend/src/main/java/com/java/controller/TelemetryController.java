package com.java.controller;

import com.java.config.ApiResponse;
import com.java.controller.dto.TelemetryIngestRequest;
import com.java.domain.service.TelemetryIngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device-telemetry")
@RequiredArgsConstructor
public class TelemetryController {
    private final TelemetryIngestService telemetryIngestService;

    @PostMapping
    public ApiResponse<?> ingest(@Valid @RequestBody TelemetryIngestRequest request) {
        telemetryIngestService.ingest(request);
        return ApiResponse.ok(null, "Đã nhận telemetry");
    }
}
