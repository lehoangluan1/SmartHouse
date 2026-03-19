package com.java.persistence.entity;

import java.io.Serializable;

public record DeviceRuntimeStateId(Long device, String capabilityCode) implements Serializable {
}