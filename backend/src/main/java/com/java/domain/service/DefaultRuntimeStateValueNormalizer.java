package com.java.domain.service;

import org.springframework.stereotype.Component;

@Component
public class DefaultRuntimeStateValueNormalizer implements RuntimeStateValueNormalizer {

    @Override
    public Object normalizeInput(Object value) {
        return normalize(value);
    }

    @Override
    public Object normalizeComparable(Object value) {
        return normalize(value);
    }

    private Object normalize(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }

        if ("true".equalsIgnoreCase(text)) {
            return true;
        }

        if ("false".equalsIgnoreCase(text)) {
            return false;
        }

        try {
            return Double.valueOf(text);
        } catch (NumberFormatException ignored) {
        }

        return text;
    }
}