package com.java.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "device_state_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStateHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, insertable = false, updatable = false)
    private Long deviceId;

    @Column(name = "capability_code", nullable = false, insertable = false, updatable = false)
    private String capabilityCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "device_id", referencedColumnName = "device_id"),
            @JoinColumn(name = "capability_code", referencedColumnName = "capability_code")
    })
    private DeviceCapabilityEntity capability;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_number")
    private Double valueNumber;

    @Column(name = "value_text")
    private String valueText;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private UserEntity changedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}