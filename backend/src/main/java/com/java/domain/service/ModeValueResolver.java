package com.java.domain.service;

public interface ModeValueResolver {

    String normalizeMode(String mode);

    void validateNormalizedMode(String mode);
}