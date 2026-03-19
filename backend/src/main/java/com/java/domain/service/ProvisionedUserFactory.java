package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.java.controller.dto.CreateUserRequest;
import com.java.domain.AuthProvider;
import com.java.domain.UserStatus;
import com.java.persistence.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProvisionedUserFactory {

    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;

    public ProvisionedUser create(CreateUserRequest request, String username) {
        boolean isLocal = request.provider() == AuthProvider.LOCAL;
        String temporaryPassword = isLocal ? passwordGenerator.generateTemporaryPassword() : null;

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(temporaryPassword != null ? passwordEncoder.encode(temporaryPassword) : null);
        user.setRole(request.systemRole());
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(isLocal);
        user.setInvitedAt(OffsetDateTime.now());

        return new ProvisionedUser(user, temporaryPassword);
    }

    public record ProvisionedUser(UserEntity user, String temporaryPassword) {
    }
}