package com.java.domain.service.dto;

public record GoogleUserInfo(
        String email,
        String name,
        String picture,
        String subject
) {}