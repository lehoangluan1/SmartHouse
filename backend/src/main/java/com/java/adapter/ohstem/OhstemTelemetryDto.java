package com.java.adapter.ohstem;

public record OhstemTelemetryDto(
    String deviceKey,
    Double temp,
    Double humidity,
    Integer shine,
    Boolean someone
) {}