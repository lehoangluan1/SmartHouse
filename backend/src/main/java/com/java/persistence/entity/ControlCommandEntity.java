package com.java.persistence.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.java.domain.CommandStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "control_commands")
@Getter
@Setter
public class ControlCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Column(name = "target", nullable = false, length = 64)
    private String target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "device_id", referencedColumnName = "device_id", insertable = false, updatable = false),
        @JoinColumn(name = "target", referencedColumnName = "capability_code", insertable = false, updatable = false)
    })
    private DeviceCapabilityEntity capability;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_number")
    private Double valueNumber;

    @Column(name = "value_text", length = 255)
    private String valueText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserEntity actor;

    @Column(name = "actor_name", length = 128)
    private String actorName;

    @Column(name = "source", length = 32)
    private String source;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "command_status")
    private CommandStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "ack_at")
    private OffsetDateTime ackAt;
}
