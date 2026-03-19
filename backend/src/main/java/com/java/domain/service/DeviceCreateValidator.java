package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.config.ForbiddenException;
import com.java.config.NotFoundException;
import com.java.controller.dto.DeviceCreateRequest;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.HomeRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeviceCreateValidator {

    private final HomeRepository homeRepository;
    private final UserRepository userRepository;
    private final DeviceSubtypePolicy deviceSubtypePolicy;

    public void validate(Long homeId, Long userId, DeviceCreateRequest request) {
        validatePermission(homeId, userId);
        validateRequest(request);
    }

    private void validatePermission(Long homeId, Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean isAdmin = user.getRole() != null
        && (
                "ADMIN".equalsIgnoreCase(user.getRole().name()) ||
                "SUPER_ADMIN".equalsIgnoreCase(user.getRole().name())
        );

        if (!isAdmin) {
            throw new ForbiddenException("You do not have permission to add devices");
        }

        homeRepository.findById(homeId)
                .orElseThrow(() -> new NotFoundException("Home not found"));
    }

    private void validateRequest(DeviceCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Invalid request");
        }

        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new BadRequestException("Device name cannot be empty");
        }

        if (request.deviceKey() == null || request.deviceKey().trim().isEmpty()) {
            throw new BadRequestException("Device key cannot be empty");
        }

        String subtype = deviceSubtypePolicy.normalizeSubtype(request.subtype());
        if (!deviceSubtypePolicy.isSupportedMonitoringSubtype(subtype)) {
            throw new BadRequestException("Subtype does not support monitoring");
        }
    }
}