package com.java.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.java.config.ApiResponse;
import com.java.controller.dto.ModeScheduleResponse;
import com.java.controller.dto.ModeScheduleUpsertRequest;
import com.java.domain.service.ModeScheduleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/homes/{homeId}/mode-schedules")
@RequiredArgsConstructor
public class ModeScheduleController {

    private final ModeScheduleService modeScheduleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModeScheduleResponse>>> list(@PathVariable Long homeId) {
        return ResponseEntity.ok(ApiResponse.ok(modeScheduleService.list(homeId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ModeScheduleResponse>> create(
            @PathVariable Long homeId,
            @RequestBody ModeScheduleUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                modeScheduleService.create(homeId, request),
                "Mode schedule created successfully"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ModeScheduleResponse>> update(
            @PathVariable Long homeId,
            @PathVariable Long id,
            @RequestBody ModeScheduleUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                modeScheduleService.update(homeId, id, request),
                "Mode schedule updated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(
            @PathVariable Long homeId,
            @PathVariable Long id
    ) {
        modeScheduleService.delete(homeId, id);
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("id", id),
                "Mode schedule deleted successfully"
        ));
    }
}