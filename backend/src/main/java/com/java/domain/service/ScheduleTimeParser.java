package com.java.domain.service;

import java.time.LocalTime;

public interface ScheduleTimeParser {

    LocalTime parseRequired(String value, String fieldName);

    LocalTime parseNullable(String value, String fieldName);
}