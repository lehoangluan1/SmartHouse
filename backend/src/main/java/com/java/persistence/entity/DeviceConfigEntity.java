package com.java.persistence.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "device_configs")
@Getter
@Setter
public class DeviceConfigEntity {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "device_id",
        foreignKey = @ForeignKey(name = "fk_device_configs_device")
    )
    private DeviceEntity device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "config_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_device_configs_config")
    )
    private ConfigEntity config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "changed_by",
        foreignKey = @ForeignKey(name = "fk_device_configs_changed_by")
    )
    private UserEntity changedBy;

    @Column(name = "changed_at", insertable = false, updatable = false)
    private OffsetDateTime changedAt;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;
}