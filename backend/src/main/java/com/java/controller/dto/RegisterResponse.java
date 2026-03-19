package com.java.controller.dto;

public record RegisterResponse(
        Long id,
        String username,
        String role,
        String status
) {
}