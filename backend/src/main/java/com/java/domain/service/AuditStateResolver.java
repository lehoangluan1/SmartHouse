package com.java.domain.service;

import com.java.controller.dto.AuditEventItem;
import com.java.domain.service.dto.AuditResolvedState;
import com.java.persistence.entity.DeviceStateHistoryEntity;
import com.java.persistence.repo.DeviceStateHistoryRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditStateResolver {

    private final DeviceStateHistoryRepository deviceStateHistoryRepository;
    private final AuditValueFormatter auditValueFormatter;

    @Transactional(readOnly = true)
    public AuditResolvedState resolve(
            Long deviceId,
            String capabilityCode,
            OffsetDateTime eventTime,
            String type,
            String fallbackFromState,
            String fallbackToState,
            String details
    ) {
        String normalizedType = auditValueFormatter.nonBlank(type, "SYSTEM_EVENT");
        String fromState = fallbackFromState;
        String toState = fallbackToState;

        if (deviceId != null && eventTime != null && !auditValueFormatter.isBlank(capabilityCode)) {
            if (auditValueFormatter.isMissingState(toState)) {
                DeviceStateHistoryEntity current =
                        deviceStateHistoryRepository
                                .findFirstByCapability_Device_IdAndCapability_CapabilityCodeAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                                        deviceId,
                                        capabilityCode,
                                        eventTime
                                );

                if (current != null) {
                    toState = auditValueFormatter.normalizeStateValue(
                            capabilityCode,
                            extractValue(current)
                    );
                }
            }

            if (auditValueFormatter.isMissingState(fromState)) {
                DeviceStateHistoryEntity previous =
                        deviceStateHistoryRepository
                                .findFirstByCapability_Device_IdAndCapability_CapabilityCodeAndCreatedAtBeforeOrderByCreatedAtDesc(
                                        deviceId,
                                        capabilityCode,
                                        eventTime
                                );

                if (previous != null) {
                    fromState = auditValueFormatter.normalizeStateValue(
                            capabilityCode,
                            extractValue(previous)
                    );
                }
            }
        }

        return AuditResolvedState.builder()
                .type(normalizedType)
                .fromState(auditValueFormatter.displayState(fromState))
                .toState(auditValueFormatter.displayState(toState))
                .details(auditValueFormatter.nonBlank(details, "-"))
                .build();
    }

    @Transactional(readOnly = true)
    public AuditResolvedState resolveTransitionAroundEvent(
            Long deviceId,
            String capabilityCode,
            OffsetDateTime eventTime,
            String type,
            String fallbackFromState,
            String fallbackToState,
            String details
    ) {
        String normalizedType = auditValueFormatter.nonBlank(type, "SYSTEM_EVENT");
        String fromState = fallbackFromState;
        String toState = fallbackToState;

        if (deviceId != null && eventTime != null && !auditValueFormatter.isBlank(capabilityCode)) {
            DeviceStateHistoryEntity previous =
                    deviceStateHistoryRepository
                            .findFirstByCapability_Device_IdAndCapability_CapabilityCodeAndCreatedAtBeforeOrderByCreatedAtDesc(
                                    deviceId,
                                    capabilityCode,
                                    eventTime
                            );

            DeviceStateHistoryEntity current =
                    deviceStateHistoryRepository
                            .findFirstByCapability_Device_IdAndCapability_CapabilityCodeAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                                    deviceId,
                                    capabilityCode,
                                    eventTime
                            );

            if (auditValueFormatter.isMissingState(fromState) && previous != null) {
                fromState = auditValueFormatter.normalizeStateValue(
                        capabilityCode,
                        extractValue(previous)
                );
            }

            if (auditValueFormatter.isMissingState(toState) && current != null) {
                toState = auditValueFormatter.normalizeStateValue(
                        capabilityCode,
                        extractValue(current)
                );
            }
        }

        return AuditResolvedState.builder()
                .type(normalizedType)
                .fromState(auditValueFormatter.displayState(fromState))
                .toState(auditValueFormatter.displayState(toState))
                .details(auditValueFormatter.nonBlank(details, "-"))
                .build();
    }

    public AuditResolvedState resolveForEvent(
            Long deviceId,
            String capabilityCode,
            OffsetDateTime eventTime,
            AuditEventItem item
    ) {
        if (item == null) {
            return AuditResolvedState.builder()
                    .type("SYSTEM_EVENT")
                    .fromState("-")
                    .toState("-")
                    .details("-")
                    .build();
        }

        return resolve(
                deviceId,
                capabilityCode,
                eventTime,
                item.getType(),
                item.getFromState(),
                item.getToState(),
                item.getDetails()
        );
    }

    private Object extractValue(DeviceStateHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getValueBoolean() != null) {
            return entity.getValueBoolean();
        }
        if (entity.getValueNumber() != null) {
            return entity.getValueNumber();
        }
        if (entity.getValueText() != null) {
            return entity.getValueText();
        }
        return null;
    }
}