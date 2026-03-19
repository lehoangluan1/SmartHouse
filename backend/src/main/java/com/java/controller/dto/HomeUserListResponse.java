package com.java.controller.dto;

import java.util.List;

public record HomeUserListResponse(
        Long homeId,
        List<HomeUserItemResponse> items
) {}
