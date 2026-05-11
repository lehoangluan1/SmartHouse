package com.java.domain.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.NotFoundException;
import com.java.domain.SystemMode;
import com.java.domain.service.dto.AutomationDecision;
import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.repo.ConfigRepository;
import com.java.persistence.repo.DeviceRepository;

@Service
public class DeviceAutomationWorker {

    private static final Logger log = LoggerFactory.getLogger(DeviceAutomationWorker.class);
    private static final String MODE_CAPABILITY = "MODE";
    private static final String TEMPERATURE_SENSOR = "TEMPERATURE";
    private static final String LIGHT_SENSOR = "LIGHT";
    private static final Set<String> FAN_SUBTYPES = Set.of("FAN", "AIR_CONDITIONER");

    private final DeviceRepository deviceRepository;
    private final ConfigRepository configRepository;
    private final HomeModeResolver homeModeResolver;
    private final SensorSnapshotService sensorSnapshotService;
    private final FanAutomationPolicy fanAutomationPolicy;
    private final LightAutomationPolicy lightAutomationPolicy;
    private final AutoControlService autoControlService;
    private final ManualHoldQueryService manualHoldQueryService;
    private final DeviceRuntimeStateService deviceRuntimeStateService;
    private final AutomationCooldownService automationCooldownService;
    private final DeviceTargetPolicy deviceTargetPolicy;

    private final ConcurrentHashMap<String, Object> targetLocks = new ConcurrentHashMap<>();

    public DeviceAutomationWorker(
            DeviceRepository deviceRepository,
            ConfigRepository configRepository,
            HomeModeResolver homeModeResolver,
            SensorSnapshotService sensorSnapshotService,
            FanAutomationPolicy fanAutomationPolicy,
            LightAutomationPolicy lightAutomationPolicy,
            AutoControlService autoControlService,
            ManualHoldQueryService manualHoldQueryService,
            DeviceRuntimeStateService deviceRuntimeStateService,
            AutomationCooldownService automationCooldownService,
            DeviceTargetPolicy deviceTargetPolicy
    ) {
        this.deviceRepository = deviceRepository;
        this.configRepository = configRepository;
        this.homeModeResolver = homeModeResolver;
        this.sensorSnapshotService = sensorSnapshotService;
        this.fanAutomationPolicy = fanAutomationPolicy;
        this.lightAutomationPolicy = lightAutomationPolicy;
        this.autoControlService = autoControlService;
        this.manualHoldQueryService = manualHoldQueryService;
        this.deviceRuntimeStateService = deviceRuntimeStateService;
        this.automationCooldownService = automationCooldownService;
        this.deviceTargetPolicy = deviceTargetPolicy;
    }

    @Transactional
    public void evaluateAndApplyOneDevice(Long deviceId) {
        evaluateAndApplyOneDevice(deviceId, "scheduler");
    }

    @Transactional
    public void evaluateAndApplyOneDevice(Long deviceId, String reason) {
        DeviceEntity device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceId));

        if (!isFan(device) && !isLight(device)) {
            return;
        }

        if (device.getHome() == null || device.getHome().getId() == null) {
            return;
        }

        Long homeId = device.getHome().getId();
        Map<String, DeviceRuntimeStateEntity> stateMap = deviceRuntimeStateService.getStateMap(device.getId());

        SystemMode fallbackMode = extractMode(device.getId(), stateMap.get(MODE_CAPABILITY));
        SystemMode mode = homeModeResolver.resolveHomeMode(homeId, fallbackMode);

        if (mode == null) {
            log.warn("Skipping automation for device {} because no valid mode could be resolved", device.getId());
            return;
        }

        log.info("AUTOMATION evaluate home={} mode={} reason={} deviceId={}",
                homeId, mode.name(), safeReason(reason), device.getId());

        if (manualHoldQueryService.isHolding(device.getId())) {
            log.debug("Skipping automation for device {} because a manual hold is active", device.getId());
            return;
        }

        ConfigEntity config = resolveActiveConfig(homeId);
        if (config == null) {
            return;
        }
        log.info("AUTOMATION config_reload configId={} home={}", config.getId(), homeId);

        Double temperature = readTemperatureSnapshot(homeId, config);
        Double light = readLightSnapshot(homeId, config);

        List<AutomationDecision> decisions = isFan(device)
                ? fanAutomationPolicy.decide(stateMap, config, temperature, mode)
                : lightAutomationPolicy.decide(stateMap, config, light, mode);

        if (mode == SystemMode.manual) {
            log.info("AUTOMATION skip reason=manual_mode home={} deviceId={}", homeId, device.getId());
        }

        Integer kMinutes = config.getKMinutes();

        for (AutomationDecision decision : decisions) {
            String normalizedTarget = normalizeTargetSafe(decision.target());
            String lockKey = device.getId() + ":" + normalizedTarget;
            Object lock = targetLocks.computeIfAbsent(lockKey, k -> new Object());

            synchronized (lock) {
                boolean immediate = isImmediateThresholdDecision(decision.reason());
                // Basic threshold reactions are intentionally immediate; K cooldown is kept for delayed/reconciliation rules.
                boolean coolingDown = !immediate && automationCooldownService.isCoolingDown(
                                device.getId(),
                                normalizedTarget,
                                decision.value(),
                                kMinutes
                        );

                log.info(
                        "AUTO_DECISION deviceId={}, subtype={}, target={}, value={}, reason={}, coolingDown={}",
                        device.getId(), device.getSubtype(), normalizedTarget, decision.value(), decision.reason(), coolingDown
                );
                logThresholdDecision(device, decision, temperature, light, config);

                if (coolingDown) {
                    continue;
                }

                boolean executed = autoControlService.execute(
                        device,
                        normalizedTarget,
                        decision.value(),
                        decision.reason()
                );

                log.info("AUTO_EXECUTED deviceId={}, target={}, value={}, executed={}",
                        device.getId(), normalizedTarget, decision.value(), executed);
            }
        }
    }

    private ConfigEntity resolveActiveConfig(Long homeId) {
        return configRepository.findFirstByHomeIdAndIsActiveTrue(homeId)
                .or(() -> configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(homeId))
                .orElse(null);
    }

    private Double readTemperatureSnapshot(Long homeId, ConfigEntity config) {
        if (config != null
                && config.getMonitoringTemperatureDevice() != null
                && config.getMonitoringTemperatureDevice().getId() != null) {
            Double value = sensorSnapshotService.latestNumericValueForDevice(
                    config.getMonitoringTemperatureDevice().getId(),
                    TEMPERATURE_SENSOR
            );
            if (value != null) {
                return value;
            }
        }
        return sensorSnapshotService.latestNumericValue(homeId, TEMPERATURE_SENSOR);
    }

    private Double readLightSnapshot(Long homeId, ConfigEntity config) {
        if (config != null
                && config.getMonitoringLightSensorDevice() != null
                && config.getMonitoringLightSensorDevice().getId() != null) {
            Double value = sensorSnapshotService.latestNumericValueForDevice(
                    config.getMonitoringLightSensorDevice().getId(),
                    LIGHT_SENSOR
            );
            if (value != null) {
                return value;
            }
        }
        return sensorSnapshotService.latestNumericValue(homeId, LIGHT_SENSOR);
    }

    private void logThresholdDecision(DeviceEntity device, AutomationDecision decision, Double temperature, Double light, ConfigEntity config) {
        if (isFan(device)) {
            if ("AUTO_TEMP_HIGH".equalsIgnoreCase(decision.reason())) {
                log.info("AUTOMATION temp value={} T_high={} action=fan_on", temperature, config.getThigh());
            } else if ("AUTO_TEMP_LOW".equalsIgnoreCase(decision.reason())) {
                log.info("AUTOMATION temp value={} T_low={} action=fan_off", temperature, config.getTlow());
            }
        } else if (isLight(device)) {
            if ("AUTO_LIGHT_LOW".equalsIgnoreCase(decision.reason())) {
                log.info("AUTOMATION light value={} L_low={} action=light_on", light, config.getLlow());
            } else if ("AUTO_LIGHT_HIGH".equalsIgnoreCase(decision.reason())) {
                log.info("AUTOMATION light value={} L_high={} action=light_off", light, config.getLhigh());
            }
        }
    }

    private boolean isImmediateThresholdDecision(String reason) {
        if (reason == null) {
            return false;
        }
        return switch (reason.trim().toUpperCase(Locale.ROOT)) {
            case "AUTO_TEMP_HIGH",
                    "AUTO_TEMP_LOW",
                    "SLEEP_TEMP_HIGH",
                    "SLEEP_TEMP_LOW",
                    "AWAY_TEMP_HIGH",
                    "AWAY_TEMP_NORMAL",
                    "AUTO_LIGHT_LOW",
                    "AUTO_LIGHT_HIGH",
                    "LIGHT_MODE_FORCE_OFF" -> true;
            default -> false;
        };
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "scheduler" : reason;
    }

    private String normalizeTargetSafe(String target) {
        try {
            return deviceTargetPolicy.normalizeTarget(target);
        } catch (RuntimeException e) {
            return target == null ? null : target.trim().toUpperCase(Locale.ROOT);
        }
    }

    private boolean isFan(DeviceEntity device) {
        return isActuator(device)
                && isFanSubtype(device)
                && deviceTargetPolicy.supportsPower(device)
                && deviceTargetPolicy.supportsSpeed(device);
    }

    private boolean isLight(DeviceEntity device) {
        return isActuator(device)
                && isSubtype(device, "LIGHT")
                && deviceTargetPolicy.supportsPower(device);
    }

    private boolean isActuator(DeviceEntity device) {
        return device != null
                && device.getDeviceClass() != null
                && "ACTUATOR".equalsIgnoreCase(device.getDeviceClass().name());
    }

    private boolean isSubtype(DeviceEntity device, String subtype) {
        return device != null
                && device.getSubtype() != null
                && subtype.equalsIgnoreCase(device.getSubtype().trim());
    }

    private boolean isFanSubtype(DeviceEntity device) {
        return device != null
                && device.getSubtype() != null
                && FAN_SUBTYPES.contains(device.getSubtype().trim().toUpperCase(Locale.ROOT));
    }

    private SystemMode extractMode(Long deviceId, DeviceRuntimeStateEntity entity) {
        if (entity == null || entity.getValueText() == null || entity.getValueText().isBlank()) {
            return null;
        }

        try {
            return SystemMode.valueOf(entity.getValueText().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            log.warn("Ignoring invalid MODE runtime state '{}' for device {}",
                    entity.getValueText(), deviceId);
            return null;
        }
    }
}
