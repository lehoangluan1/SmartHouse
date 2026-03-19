package com.java.controller;

import com.java.config.ApiResponse;
import com.java.domain.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/homes/{homeId}")
    public ApiResponse<?> summary(@PathVariable Long homeId) {
        return ApiResponse.ok(dashboardService.summary(homeId));
    }
}
