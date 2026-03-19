package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.config.JwtService;
import com.java.domain.SystemUserRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtService jwtService;

    @Override
    public String issue(Long userId, String username, SystemUserRole role) {
        return jwtService.generateToken(userId, username, role);
    }
}