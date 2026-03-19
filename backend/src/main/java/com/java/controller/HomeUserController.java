package com.java.controller;

import org.springframework.web.bind.annotation.*;

import com.java.config.ApiResponse;
import com.java.controller.dto.HomeUserItemResponse;
import com.java.controller.dto.HomeUserListResponse;
import com.java.controller.dto.SetHomeUserPasswordRequest;
import com.java.domain.service.HomeUserManagementService;
import com.java.domain.service.dto.AddHomeUserRequest;
import com.java.domain.service.dto.UpdateHomeUserRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/homes/{homeId}/users")
@RequiredArgsConstructor
public class HomeUserController {

    private final HomeUserManagementService homeUserManagementService;

    @GetMapping
    public ApiResponse<HomeUserListResponse> getUsers(@PathVariable Long homeId) {
        return ApiResponse.ok(homeUserManagementService.getUsers(homeId));
    }

    @PostMapping
    public ApiResponse<HomeUserItemResponse> addUser(
            @PathVariable Long homeId,
            @Valid @RequestBody AddHomeUserRequest request
    ) {
        return ApiResponse.ok(homeUserManagementService.addUser(homeId, request));
    }

    @PatchMapping("/{userId}")
    public ApiResponse<HomeUserItemResponse> updateUser(
            @PathVariable Long homeId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateHomeUserRequest request
    ) {
        return ApiResponse.ok(homeUserManagementService.updateUser(homeId, userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeUser(
            @PathVariable Long homeId,
            @PathVariable Long userId
    ) {
        homeUserManagementService.removeUser(homeId, userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{userId}/set-password")
    public ApiResponse<Void> setPassword(
            @PathVariable Long homeId,
            @PathVariable Long userId,
            @Valid @RequestBody SetHomeUserPasswordRequest request
    ) {
        homeUserManagementService.setPassword(homeId, userId, request);
        return ApiResponse.ok(null);
    }
}