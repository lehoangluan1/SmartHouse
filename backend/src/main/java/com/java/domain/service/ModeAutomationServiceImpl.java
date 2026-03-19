package com.java.domain.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

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
public class ModeAutomationServiceImpl implements ModeAutomationService {

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

    public ModeAutomationServiceImpl(
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

    @Override
    @Transactional
    public void evaluateAndApply(Long deviceId) {
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

        SystemMode fallbackMode = extractMode(stateMap.get("MODE"));
        SystemMode mode = homeModeResolver.resolveHomeMode(homeId, fallbackMode);

        if (mode == SystemMode.manual && manualHoldQueryService.isHolding(device.getId())) {
            return;
        }

        ConfigEntity config = configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(homeId).orElse(null);
        if (config == null) {
            return;
        }

        List<AutomationDecision> decisions = isFan(device)
                ? fanAutomationPolicy.decide(
                        stateMap,
                        config,
                        sensorSnapshotService.latestNumericValue(homeId, "TEMPERATURE"),
                        mode
                )
                : lightAutomationPolicy.decide(
                        stateMap,
                        config,
                        sensorSnapshotService.latestNumericValue(homeId, "LIGHT"),
                        mode
                );

        for (AutomationDecision decision : decisions) {
            if (automationCooldownService.isCoolingDown(homeId, stateMap, decision.target())) {
                continue;
            }

            autoControlService.autoControl(
                    device.getId(),
                    decision.target(),
                    decision.value(),
                    decision.reason()
            );
        }
    }

    @Override
    @Transactional
    public void evaluateAllByHome(Long homeId) {
        deviceRepository.findByHomeId(homeId).stream()
                .filter(device -> isFan(device) || isLight(device))
                .forEach(device -> evaluateAndApply(device.getId()));
    }

    private boolean isFan(DeviceEntity device) {
        return isActuator(device)
                && isSubtype(device, "FAN")
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

    private SystemMode extractMode(DeviceRuntimeStateEntity entity) {
        if (entity == null || entity.getValueText() == null || entity.getValueText().isBlank()) {
            return SystemMode.auto;
        }

        try {
            return SystemMode.valueOf(entity.getValueText().trim().toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            return SystemMode.auto;
        }
    }
}