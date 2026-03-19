package com.java.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.java.config.ApiResponse;
import com.java.controller.dto.LoginRequest;
import com.java.controller.dto.LoginResponse;
import com.java.controller.dto.RefreshTokenRequest;
import com.java.controller.dto.RefreshTokenResponse;
import com.java.domain.service.AuthenticationService;
import com.java.domain.service.TokenRefreshFacade;
import com.java.domain.service.dto.AuthenticationCommand;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final TokenRefreshFacade tokenRefreshFacade;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authenticationService.login(
                new AuthenticationCommand(
                        request.provider(),
                        request.username(),
                        request.password(),
                        request.authorizationCode(),
                        request.redirectUri()
                )
        ));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(tokenRefreshFacade.refresh(request.refreshToken()));
    }
}