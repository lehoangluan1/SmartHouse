package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.controller.dto.CreateUserRequest;
import com.java.controller.dto.HomeAssignmentMode;
import com.java.domain.HomeUserRole;
import com.java.domain.SystemUserRole;

@Component
public class UserProvisionRequestValidator {

    public void validate(CreateUserRequest request) {
        if (request == null) {
            throw new BadRequestException("Request must not be null");
        }

        if (request.systemRole() == SystemUserRole.SUPER_ADMIN) {
            throw new BadRequestException("Cannot create SUPER_ADMIN from this screen");
        }

        if (request.homeAssignmentMode() == HomeAssignmentMode.CREATE_NEW) {
            validateCreateNewHome(request);
            return;
        }

        validateJoinExistingHome(request);
    }

    private void validateCreateNewHome(CreateUserRequest request) {
        if (request.systemRole() == SystemUserRole.INSTALLER) {
            throw new BadRequestException("INSTALLER is not allowed to create a new home");
        }

        if (request.homeName() == null || request.homeName().isBlank()) {
            throw new BadRequestException("Home name must not be blank");
        }

        if (request.homeRole() != HomeUserRole.OWNER) {
            throw new BadRequestException("User creating a new home must have role OWNER");
        }
    }

    private void validateJoinExistingHome(CreateUserRequest request) {
        if (request.homeId() == null) {
            throw new BadRequestException("homeId must not be blank when joining a home");
        }

        if (request.homeRole() == null) {
            throw new BadRequestException("Home role is required");
        }

        if (request.systemRole() == SystemUserRole.INSTALLER
                && request.homeRole() != HomeUserRole.TECHNICIAN
                && request.homeRole() != HomeUserRole.VIEWER) {
            throw new BadRequestException("INSTALLER is only compatible with TECHNICIAN or VIEWER");
        }
    }
}