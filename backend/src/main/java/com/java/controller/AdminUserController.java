package com.java.controller;

import org.springframework.web.bind.annotation.*;

import com.java.config.ApiResponse;
import com.java.controller.dto.AdminResetPasswordResponse;
import com.java.controller.dto.AdminUpdateUserRequest;
import com.java.controller.dto.AdminUserItemResponse;
import com.java.controller.dto.AdminUserListResponse;
import com.java.controller.dto.CreateUserRequest;
import com.java.controller.dto.LinkUserAuthProviderRequest;
import com.java.controller.dto.UserAuthProviderListResponse;
import com.java.controller.dto.UserProvisionResponse;
import com.java.domain.service.AdminUserAuthProviderService;
import com.java.domain.service.AdminUserManagementService;
import com.java.domain.service.UserProvisioningService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserProvisioningService userProvisioningService;
    private final AdminUserManagementService adminUserManagementService;
    private final AdminUserAuthProviderService adminUserAuthProviderService;

    @PostMapping
    public ApiResponse<UserProvisionResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userProvisioningService.createUser(request));
    }

    @GetMapping
    public ApiResponse<AdminUserListResponse> getUsers() {
        return ApiResponse.ok(adminUserManagementService.getUsers());
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserItemResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserManagementService.getUser(userId));
    }

    @PatchMapping("/{userId}")
    public ApiResponse<AdminUserItemResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        return ApiResponse.ok(adminUserManagementService.updateUser(userId, request));
    }

    @PostMapping("/{userId}/reset-password")
    public ApiResponse<AdminResetPasswordResponse> resetPassword(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserManagementService.resetPassword(userId));
    }

    @GetMapping("/{userId}/auth-providers")
    public ApiResponse<UserAuthProviderListResponse> getUserAuthProviders(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserAuthProviderService.getProviders(userId));
    }

    @PostMapping("/{userId}/auth-providers")
    public ApiResponse<UserAuthProviderListResponse> linkUserAuthProvider(
            @PathVariable Long userId,
            @Valid @RequestBody LinkUserAuthProviderRequest request
    ) {
        return ApiResponse.ok(adminUserAuthProviderService.linkProvider(userId, request));
    }
}