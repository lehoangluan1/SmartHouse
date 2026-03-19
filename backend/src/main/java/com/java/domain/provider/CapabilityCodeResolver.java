package com.java.domain.provider;

public interface CapabilityCodeResolver {
    String resolve(String rawTarget, String fallbackType);
}
