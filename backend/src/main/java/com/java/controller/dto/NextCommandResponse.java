package com.java.controller.dto;

public record NextCommandResponse(
        Long id,
        String target,
        String value
) {}