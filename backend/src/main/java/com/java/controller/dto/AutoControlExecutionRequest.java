package com.java.controller.dto;

public record AutoControlExecutionRequest(
        Long deviceId,
        String target,
        String value,
        String method
) implements ControlExecutionRequest {
}