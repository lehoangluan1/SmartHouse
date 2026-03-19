package com.java.persistence.entity;

import java.time.LocalTime;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "schedules")
@Getter
@Setter
public class ScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

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

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "days_mask", nullable = false)
    private Integer daysMask;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @SuppressWarnings("unused")
    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (daysMask == null) daysMask = 127;
        if (enabled == null) enabled = true;
        if (priority == null) priority = 0;
    }

    @SuppressWarnings("unused")
    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}