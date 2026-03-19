package com.java.domain.service;

public interface RuntimeStateValueNormalizer {
    Object normalizeInput(Object value);
    Object normalizeComparable(Object value);
}