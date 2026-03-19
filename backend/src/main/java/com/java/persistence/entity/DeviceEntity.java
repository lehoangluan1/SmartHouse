package com.java.persistence.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.java.domain.DeviceClass;
import com.java.domain.EntityStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "devices")
@Getter
@Setter
public class DeviceEntity extends BaseEntityTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "home_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_devices_home")
    )
    private HomeEntity home;

    @Column(name = "device_key", nullable = false, unique = true, length = 64)
    private String deviceKey;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "class", nullable = false, columnDefinition = "device_class")
    private DeviceClass deviceClass;

    @Column(name = "subtype", nullable = false, length = 64)
    private String subtype;

    @Column(name = "room_name", length = 64)
    private String roomName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "entity_status")
    private EntityStatus status;

    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "installed_by",
        foreignKey = @ForeignKey(name = "fk_devices_installed_by")
    )
    private UserEntity installedBy;

    @Column(name = "firmware_version", length = 64)
    private String firmwareVersion;
}