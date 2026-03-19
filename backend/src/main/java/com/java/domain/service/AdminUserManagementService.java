package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.AdminResetPasswordResponse;
import com.java.controller.dto.AdminUpdateUserRequest;
import com.java.controller.dto.AdminUserItemResponse;
import com.java.controller.dto.AdminUserListResponse;
import com.java.domain.AuthProvider;
import com.java.domain.UserEventType;
import com.java.domain.service.dto.UserOperationEvent;
import com.java.persistence.entity.UserAuthProviderEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.HomeUserRepository;
import com.java.persistence.repo.UserAuthProviderRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final HomeUserRepository homeUserRepository;

    private final CurrentUserService currentUserService;
    private final UserEventOutboxService userEventOutboxService;

    @Transactional(readOnly = true)
    public AdminUserListResponse getUsers() {
        List<AdminUserItemResponse> items = userRepository.findAll()
                .stream()
                .map(user -> {
                    Long homeId = homeUserRepository
                            .findPrimaryHomeIdByUserId(user.getId())
                            .orElse(null);
                    return toItem(user, homeId);
                })
                .toList();

        return new AdminUserListResponse(items);
    }

    @Transactional(readOnly = true)
    public AdminUserItemResponse getUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User does not exist"));
        Long homeId = homeUserRepository.findPrimaryHomeIdByUserId(userId).orElse(null);
        return toItem(user, homeId);
    }

    @Transactional
    public AdminUserItemResponse updateUser(Long userId, AdminUpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        String oldRole = user.getRole() != null ? user.getRole().name() : null;
        String oldStatus = user.getStatus() != null ? user.getStatus().name() : null;

        user.setRole(request.role());
        user.setStatus(request.status());

        Long homeId = homeUserRepository.findPrimaryHomeIdByUserId(userId).orElse(null);
        AuthProvider provider = resolveProvider(user.getId());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", "adminUpdateUser");
        metadata.put("oldRole", oldRole);
        metadata.put("newRole", user.getRole() != null ? user.getRole().name() : null);
        metadata.put("oldStatus", oldStatus);
        metadata.put("newStatus", user.getStatus() != null ? user.getStatus().name() : null);
        metadata.put("temporaryPassword", null);

        userEventOutboxService.enqueue(
                UserEventType.ADMIN_USER_UPDATED,
                user.getId(),
                new UserOperationEvent(
                        UserEventType.ADMIN_USER_UPDATED,
                        homeId,
                        user.getId(),
                        user.getUsername(),
                        null,
                        null,
                        null,
                        provider != null ? provider.name() : null,
                        currentUserService.getCurrentUserId(),
                        OffsetDateTime.now(),
                        metadata
                )
        );

        return toItem(user, homeId);
    }

    @Transactional
    public AdminResetPasswordResponse resetPassword(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        boolean hasLocalProvider = userAuthProviderRepository.existsByUserIdAndProvider(userId, AuthProvider.LOCAL);
        if (!hasLocalProvider) {
            throw new BadRequestException("This user does not use a LOCAL account so password cannot be reset");
        }

        String temporaryPassword = passwordGenerator.generateTemporaryPassword();

        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);

        Long homeId = homeUserRepository.findPrimaryHomeIdByUserId(userId).orElse(null);
        AuthProvider provider = resolveProvider(user.getId());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", "adminResetPassword");
        metadata.put("temporaryPassword", temporaryPassword);
        metadata.put("mustChangePassword", true);

        userEventOutboxService.enqueue(
                UserEventType.ADMIN_USER_PASSWORD_RESET,
                user.getId(),
                new UserOperationEvent(
                        UserEventType.ADMIN_USER_PASSWORD_RESET,
                        homeId,
                        user.getId(),
                        user.getUsername(),
                        null,
                        null,
                        null,
                        provider != null ? provider.name() : null,
                        currentUserService.getCurrentUserId(),
                        OffsetDateTime.now(),
                        metadata
                )
        );

        return new AdminResetPasswordResponse(
                user.getId(),
                user.getUsername(),
                true,
                true
        );
    }

    private AdminUserItemResponse toItem(UserEntity user, Long homeId) {
        AuthProvider provider = userAuthProviderRepository.findFirstByUserIdOrderByLinkedAtAsc(user.getId())
                .map(UserAuthProviderEntity::getProvider)
                .orElse(null);

        return new AdminUserItemResponse(
                user.getId(),
                user.getUsername(),
                provider,
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword(),
                homeId,
                user.getInvitedAt()
        );
    }

    private AuthProvider resolveProvider(Long userId) {
        return userAuthProviderRepository.findFirstByUserIdOrderByLinkedAtAsc(userId)
                .map(UserAuthProviderEntity::getProvider)
                .orElse(null);
    }
}