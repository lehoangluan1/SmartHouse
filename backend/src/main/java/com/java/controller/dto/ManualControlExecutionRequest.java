package com.java.controller.dto;

public record ManualControlExecutionRequest(
        Long deviceId,
        ControlRequest request
) implements ControlExecutionRequest {
}