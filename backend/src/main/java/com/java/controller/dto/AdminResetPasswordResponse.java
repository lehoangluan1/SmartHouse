package com.java.controller.dto;

public record AdminResetPasswordResponse(
        Long userId,
        String username,
        boolean mustChangePassword,
        boolean notificationQueued
) {}
