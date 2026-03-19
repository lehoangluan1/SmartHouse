package com.java.config;

import java.time.OffsetDateTime;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiError {
    private String code;                  // BAD_REQUEST, NOT_FOUND...
    private String error;                 // short_message
    private String message;            
    private Map<String, String> details;  // field errors if any
    private OffsetDateTime timestamp;
}