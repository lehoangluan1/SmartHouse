package com.java.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityId implements Serializable {

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "capability_code", length = 64)
    private String capabilityCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CapabilityId that)) return false;
        return Objects.equals(deviceId, that.deviceId)
            && Objects.equals(capabilityCode, that.capabilityCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, capabilityCode);
    }
    
}