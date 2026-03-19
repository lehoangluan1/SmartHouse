package com.java.domain.service;

public record ScheduleTypedValue(Boolean boolValue, Double numberValue, String textValue) {

    public String asString() {
        if (boolValue != null) {
            return String.valueOf(boolValue);
        }
        if (numberValue != null) {
            return String.valueOf(numberValue);
        }
        return textValue;
    }
}