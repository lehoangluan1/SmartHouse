package com.java.controller;

import com.java.config.ApiResponse;
import com.java.controller.dto.ConfigUpsertRequest;
import com.java.domain.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/homes/{homeId}/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public ApiResponse<?> list(@PathVariable Long homeId) {
        return ApiResponse.ok(configService.getByHome(homeId));
    }

    @GetMapping("/active")
    public ApiResponse<?> getActive(@PathVariable Long homeId) {
        return ApiResponse.ok(configService.getActiveByHome(homeId));
    }

    @PostMapping
    public ApiResponse<?> create(
            @PathVariable Long homeId,
            @Valid @RequestBody ConfigUpsertRequest request
    ) {
        return ApiResponse.ok(configService.upsert(homeId, null, request));
    }

    @PutMapping("/{configId}")
    public ApiResponse<?> update(
            @PathVariable Long homeId,
            @PathVariable Long configId,
            @Valid @RequestBody ConfigUpsertRequest request
    ) {
        return ApiResponse.ok(configService.upsert(homeId, configId, request));
    }

    @PutMapping("/{configId}/activate")
    public ApiResponse<?> activate(
            @PathVariable Long homeId,
            @PathVariable Long configId
    ) {
        return ApiResponse.ok(configService.activate(homeId, configId));
    }

    @DeleteMapping("/{configId}")
    public ApiResponse<?> delete(
            @PathVariable Long homeId,
            @PathVariable Long configId
    ) {
        configService.delete(homeId, configId);
        return ApiResponse.ok("Deleted");
    }
}