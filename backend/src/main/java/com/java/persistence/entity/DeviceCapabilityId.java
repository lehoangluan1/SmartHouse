package com.java.persistence.entity;

import java.io.Serializable;

public record DeviceCapabilityId(Long device, String capabilityCode) implements Serializable {
}