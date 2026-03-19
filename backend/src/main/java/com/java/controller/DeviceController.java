package com.java.controller;

import com.java.config.ApiResponse;
import com.java.controller.dto.DeviceCreateRequest;
import com.java.domain.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping("/home/{homeId}")
    public ApiResponse<?> getByHome(@PathVariable Long homeId) {
        return ApiResponse.ok(deviceService.getByHome(homeId));
    }

    @PostMapping("/home/{homeId}")
    public ApiResponse<?> create(
            @PathVariable Long homeId,
            @RequestParam Long userId,
            @Valid @RequestBody DeviceCreateRequest request
    ) {
        return ApiResponse.ok(deviceService.createForHome(homeId, userId, request));
    }

    @GetMapping("/{deviceId}")
    public ApiResponse<?> getOne(@PathVariable Long deviceId) {
        return ApiResponse.ok(deviceService.getById(deviceId));
    }

    @GetMapping("/{deviceId}/state")
    public ApiResponse<?> getState(@PathVariable Long deviceId) {
        return ApiResponse.ok(deviceService.getState(deviceId));
    }
}