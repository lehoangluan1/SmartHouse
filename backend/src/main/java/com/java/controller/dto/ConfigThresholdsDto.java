package com.java.controller.dto;

public record ConfigThresholdsDto(
        Double tHigh,
        Double tLow,
        Integer lLow,
        Integer lHigh,
        Double tSleepHigh,
        Double tSleepLow,
        Double tAwayHigh,
        Double tCritical,
        Integer n,
        Integer m,
        Integer tHold,
        Integer dPresent,
        Integer k,
        Integer autoFanSpeed,
        Integer sleepFanSpeed,
        Integer awayFanSpeed
) {
}