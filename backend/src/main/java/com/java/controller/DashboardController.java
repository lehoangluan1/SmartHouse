package com.java.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.java.config.ApiResponse;
import com.java.domain.service.DashboardRealtimeService;
import com.java.domain.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardRealtimeService dashboardRealtimeService;

    @GetMapping("/homes/{homeId}")
    public ApiResponse<?> summary(@PathVariable Long homeId) {
        return ApiResponse.ok(dashboardService.summary(homeId));
    }

    @GetMapping(
        value = "/homes/{homeId}/stream",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(@PathVariable Long homeId) {
        return dashboardRealtimeService.subscribe(homeId);
    }
}