package com.java.controller;

import com.java.config.ApiResponse;
import com.java.controller.dto.ScheduleUpsertRequest;
import com.java.domain.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @PostMapping
    public ApiResponse<?> upsert(@Valid @RequestBody ScheduleUpsertRequest request) {
        return ApiResponse.ok(scheduleService.upsert(request));
    }

    @GetMapping("/devices/{deviceId}")
    public ApiResponse<?> getByDevice(@PathVariable Long deviceId) {
        return ApiResponse.ok(scheduleService.getByDevice(deviceId));
    }
}