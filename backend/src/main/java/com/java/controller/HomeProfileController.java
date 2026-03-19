package com.java.controller;

import org.springframework.web.bind.annotation.*;

import com.java.config.ApiResponse;
import com.java.controller.dto.ActivateHomeProfileResponse;
import com.java.domain.service.HomeProfileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/home-profiles")
@RequiredArgsConstructor
public class HomeProfileController {

    private final HomeProfileService homeProfileService;

    @PostMapping("/{homeId}/activate")
    public ApiResponse<ActivateHomeProfileResponse> activate(@PathVariable Long homeId) {
        return ApiResponse.ok(homeProfileService.activate(homeId));
    }
}