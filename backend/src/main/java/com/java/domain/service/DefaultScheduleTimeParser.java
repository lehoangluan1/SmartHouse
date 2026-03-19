package com.java.domain.service;

import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;

@Component
public class DefaultScheduleTimeParser implements ScheduleTimeParser {

    @Override
    public LocalTime parseRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }

        try {
            return LocalTime.parse(value.trim());
        } catch (Exception e) {
            throw new BadRequestException(fieldName + " must be in format HH:mm or HH:mm:ss");
        }
    }

    @Override
    public LocalTime parseNullable(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(value.trim());
        } catch (Exception e) {
            throw new BadRequestException(fieldName + " must be in format HH:mm or HH:mm:ss");
        }
    }
}