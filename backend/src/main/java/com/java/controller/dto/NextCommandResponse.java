package com.java.controller.dto;

public record NextCommandResponse(
        Long id,
        Long homeId,
        String deviceKey,
        String target,
        String value,
        String source
) {}
