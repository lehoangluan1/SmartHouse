package com.java.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "configs")
@Getter
@Setter
public class ConfigEntity extends BaseEntityTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "home_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_configs_home")
    )
    private HomeEntity home;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "created_by",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_configs_created_by")
    )
    private UserEntity createdBy;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "thigh")
    private Double thigh;

    @Column(name = "tlow")
    private Double tlow;

    @Column(name = "lhigh")
    private Integer lhigh;

    @Column(name = "llow")
    private Integer llow;

    @Column(name = "tsleep_high")
    private Double tsleepHigh;

    @Column(name = "tsleep_low")
    private Double tsleepLow;

    @Column(name = "taway_high")
    private Double tawayHigh;

    @Column(name = "tcritical")
    private Double tcritical;

    @Column(name = "n_minutes")
    private Integer nMinutes;

    @Column(name = "m_minutes")
    private Integer mMinutes;

    @Column(name = "thold_minutes")
    private Integer tholdMinutes;

    @Column(name = "dpresent")
    private Integer dpresent;

    @Column(name = "k_minutes")
    private Integer kMinutes;

    @Column(name = "auto_fan_speed")
    private Integer autoFanSpeed;

    @Column(name = "sleep_fan_speed")
    private Integer sleepFanSpeed;

    @Column(name = "away_fan_speed")
    private Integer awayFanSpeed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_temperature_device_id")
    private DeviceEntity monitoringTemperatureDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_humidity_device_id")
    private DeviceEntity monitoringHumidityDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_light_sensor_device_id")
    private DeviceEntity monitoringLightSensorDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_motion_device_id")
    private DeviceEntity monitoringMotionDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_fan_device_id")
    private DeviceEntity monitoringFanDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_light_device_id")
    private DeviceEntity monitoringLightDevice;
}