package com.java.controller.dto;

public record ControlExecutionResult(
        String flow,
        boolean executed,
        String status,
        ControlCommandResponse commandResponse
) {
    public static ControlExecutionResult manual(ControlCommandResponse response) {
        return new ControlExecutionResult(
                "MANUAL",
                true,
                "OK",
                response
        );
    }

    public static ControlExecutionResult autoExecuted() {
        return new ControlExecutionResult(
                "AUTO",
                true,
                "OK",
                null
        );
    }

    public static ControlExecutionResult autoNoOp() {
        return new ControlExecutionResult(
                "AUTO",
                false,
                "NO_OP",
                null
        );
    }
}