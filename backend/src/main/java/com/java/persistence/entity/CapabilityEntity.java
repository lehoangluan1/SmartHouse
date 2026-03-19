package com.java.persistence.entity;

import java.time.OffsetDateTime;

import com.java.domain.CapabilityValueType;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "device_capabilities")
@Getter
@Setter
public class CapabilityEntity {

    @EmbeddedId
    private CapabilityId id;

    @MapsId("deviceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Column(name = "value_type", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private CapabilityValueType valueType;

    @Column(name = "is_writable", nullable = false)
    private Boolean writable = Boolean.TRUE;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public String getCapabilityCode() {
        return id != null ? id.getCapabilityCode() : null;
    }

    public void setCapabilityCode(String capabilityCode) {
        if (id == null) {
            id = new CapabilityId();
        }
        id.setCapabilityCode(capabilityCode);
    }
}