package com.java.controller.dto;

import java.util.List;

public record AdminUserListResponse(
        List<AdminUserItemResponse> items
) {}