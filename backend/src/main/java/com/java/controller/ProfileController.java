package com.java.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.java.config.ApiResponse;
import com.java.controller.dto.ChangePasswordRequest;
import com.java.controller.dto.MyProfileResponse;
import com.java.domain.service.ProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ApiResponse<MyProfileResponse> me(Authentication authentication) {
        return ApiResponse.ok(profileService.getMyProfile(authentication.getName()));
    }

    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        profileService.changeMyPassword(
                authentication.getName(),
                request.currentPassword(),
                request.newPassword()
        );
        return ApiResponse.ok(null);
    }
}