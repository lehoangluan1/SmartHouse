package com.java.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.java.config.ApiResponse;
import com.java.controller.dto.LinkCurrentGoogleAccountRequest;
import com.java.controller.dto.UserAuthProviderListResponse;
import com.java.domain.service.AdminUserAuthProviderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/account/auth-providers")
@RequiredArgsConstructor
public class AccountAuthProviderController {

    private final AdminUserAuthProviderService adminUserAuthProviderService;

    @GetMapping
    public ApiResponse<UserAuthProviderListResponse> getCurrentUserProviders() {
        return ApiResponse.ok(adminUserAuthProviderService.getCurrentUserProviders());
    }

    @PostMapping("/google/link")
    public ApiResponse<UserAuthProviderListResponse> linkGoogleForCurrentUser(
            @Valid @RequestBody LinkCurrentGoogleAccountRequest request
    ) {
        return ApiResponse.ok(adminUserAuthProviderService.linkGoogleForCurrentUser(request));
    }
}