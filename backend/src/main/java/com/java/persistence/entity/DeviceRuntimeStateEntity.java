package com.java.persistence.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "device_runtime_state")
@IdClass(DeviceRuntimeStateId.class)
@Getter
@Setter
public class DeviceRuntimeStateEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Id
    @Column(name = "capability_code", nullable = false, length = 64)
    private String capabilityCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "device_id", referencedColumnName = "device_id", insertable = false, updatable = false),
        @JoinColumn(name = "capability_code", referencedColumnName = "capability_code", insertable = false, updatable = false)
    })
    private DeviceCapabilityEntity capability;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_number")
    private Double valueNumber;

    @Column(name = "value_text", length = 255)
    private String valueText;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}