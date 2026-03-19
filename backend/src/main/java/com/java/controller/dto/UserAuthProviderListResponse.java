package com.java.controller.dto;

import java.util.List;

public record UserAuthProviderListResponse(
        Long userId,
        String username,
        List<UserAuthProviderItemResponse> providers
) {}