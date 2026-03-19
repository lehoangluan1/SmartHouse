package com.java.domain.service;

import com.java.config.BadRequestException;
import com.java.controller.dto.ControlRequest;
import com.java.domain.DeviceTarget;
import com.java.domain.SystemMode;
import com.java.domain.provider.DeviceSubtypeResolver;
import com.java.domain.provider.DeviceTargetResolver;
import com.java.domain.provider.DeviceTargetSupportPolicy;
import com.java.domain.provider.DeviceTargetValueParser;
import com.java.persistence.entity.DeviceEntity;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DeviceTargetPolicy {

    private final DeviceTargetResolver targetResolver;
    private final DeviceSubtypeResolver subtypeResolver;
    private final List<DeviceTargetValueParser> valueParsers;
    private final List<DeviceTargetSupportPolicy> supportPolicies;

    public DeviceTargetPolicy(
            DeviceTargetResolver targetResolver,
            DeviceSubtypeResolver subtypeResolver,
            List<DeviceTargetValueParser> valueParsers,
            List<DeviceTargetSupportPolicy> supportPolicies
    ) {
        this.targetResolver = targetResolver;
        this.subtypeResolver = subtypeResolver;
        this.valueParsers = valueParsers;
        this.supportPolicies = supportPolicies;
    }

    public void validateManualRequest(ControlRequest request) {
        if (request == null) {
            throw new BadRequestException("Invalid request");
        }
        requireText(request.target(), "Target must not be blank");
        requireText(request.value(), "Value must not be blank");
    }

    public void validateAutoRequest(String target, String value) {
        requireText(target, "Target must not be blank");
        requireText(value, "Value must not be blank");
    }

    public String normalizeTarget(String target) {
        return targetResolver.normalize(target);
    }

    public Object normalizeValue(String target, String value) {
        DeviceTarget resolvedTarget = targetResolver.resolve(target);

        return valueParsers.stream()
                .filter(parser -> parser.supports(resolvedTarget))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No parser found for target " + resolvedTarget))
                .parse(value);
    }

    public void validateTargetForDevice(DeviceEntity device, String target) {
        if (device == null) {
            throw new BadRequestException("Invalid device");
        }

        DeviceTarget resolvedTarget = targetResolver.resolve(target);
        String subtype = subtypeResolver.normalize(device.getSubtype());

        Set<DeviceTarget> supportedTargets = supportPolicies.stream()
                .filter(policy -> policy.supports(subtype))
                .findFirst()
                .map(DeviceTargetSupportPolicy::supportedTargets)
                .orElseThrow(() -> new BadRequestException(
                        "Chưa cấu hình target cho subtype: " + (subtype.isBlank() ? "<empty>" : subtype)));

        if (!supportedTargets.contains(resolvedTarget)) {
            throw new BadRequestException(
                    "Device subtype " + subtype + " does not support target " + resolvedTarget.name());
        }
    }

    public boolean supportsModeSync(DeviceEntity device) {
        return supportsTarget(device, DeviceTarget.MODE);
    }

    public boolean supportsPower(DeviceEntity device) {
        return supportsTarget(device, DeviceTarget.POWER);
    }

    public boolean supportsSpeed(DeviceEntity device) {
        return supportsTarget(device, DeviceTarget.SPEED);
    }

    public boolean supportsBrightness(DeviceEntity device) {
        return supportsTarget(device, DeviceTarget.BRIGHTNESS);
    }

    public SystemMode parseSystemMode(String value) {
        requireText(value, "Mode must not be blank");

        return switch (value.trim().toLowerCase()) {
            case "auto" -> SystemMode.auto;
            case "manual" -> SystemMode.manual;
            case "sleep" -> SystemMode.sleep;
            case "away" -> SystemMode.away;
            default -> throw new BadRequestException("Invalid mode: " + value);
        };
    }

    private boolean supportsTarget(DeviceEntity device, DeviceTarget target) {
        if (device == null) {
            return false;
        }

        String subtype = subtypeResolver.normalize(device.getSubtype());

        return supportPolicies.stream()
                .filter(policy -> policy.supports(subtype))
                .findFirst()
                .map(DeviceTargetSupportPolicy::supportedTargets)
                .map(targets -> targets.contains(target))
                .orElse(false);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }
}