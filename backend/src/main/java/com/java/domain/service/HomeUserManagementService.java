package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.HomeUserItemResponse;
import com.java.controller.dto.HomeUserListResponse;
import com.java.controller.dto.SetHomeUserPasswordRequest;
import com.java.domain.AuthProvider;
import com.java.domain.HomeUserRole;
import com.java.domain.UserEventType;
import com.java.domain.provider.HomeUserPrimaryEligibilityPolicy;
import com.java.domain.provider.HomeUserProviderResolver;
import com.java.domain.provider.HomeUserRoleChangePolicy;
import com.java.domain.provider.HomeUserViewAssembler;
import com.java.domain.service.dto.AddHomeUserRequest;
import com.java.domain.service.dto.UpdateHomeUserRequest;
import com.java.domain.service.dto.UserOperationEvent;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.entity.HomeUserEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.HomeRepository;
import com.java.persistence.repo.HomeUserRepository;
import com.java.persistence.repo.UserAuthProviderRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeUserManagementService {

    private final HomeRepository homeRepository;
    private final UserRepository userRepository;
    private final HomeUserRepository homeUserRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final PasswordEncoder passwordEncoder;

    private final HomeUserRoleChangePolicy homeUserRoleChangePolicy;
    private final HomeUserPrimaryEligibilityPolicy homeUserPrimaryEligibilityPolicy;
    private final HomeUserProviderResolver homeUserProviderResolver;
    private final HomeUserViewAssembler homeUserViewAssembler;

    private final CurrentUserService currentUserService;
    private final UserEventOutboxService userEventOutboxService;

    @Transactional(readOnly = true)
    public HomeUserListResponse getUsers(Long homeId) {
        ensureHomeExists(homeId);

        List<HomeUserItemResponse> items = homeUserRepository.findAllByHomeId(homeId)
                .stream()
                .map(entity -> homeUserViewAssembler.toItem(
                        entity,
                        homeUserProviderResolver.resolveProviderName(entity.getUser().getId())
                ))
                .toList();

        return new HomeUserListResponse(homeId, items);
    }

    @Transactional
    public HomeUserItemResponse addUser(Long homeId, AddHomeUserRequest request) {
        HomeEntity home = homeRepository.findById(homeId)
                .orElseThrow(() -> new BadRequestException("Home not found"));

        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (homeUserRepository.existsByHomeIdAndUserId(homeId, user.getId())) {
            throw new BadRequestException("User is already in this home");
        }

        HomeUserRole roleInHome = Optional.ofNullable(request.roleInHome())
                .orElse(HomeUserRole.RESIDENT);

        boolean allowProfileActivation = Boolean.TRUE.equals(request.allowProfileActivation());
        boolean isPrimary = Boolean.TRUE.equals(request.isPrimary());

        homeUserPrimaryEligibilityPolicy.validate(roleInHome, isPrimary, allowProfileActivation);

        if (roleInHome == HomeUserRole.OWNER) {
            homeUserRoleChangePolicy.demoteExistingOwner(homeId, user.getId());
        }

        if (isPrimary) {
            homeUserRepository.clearPrimaryByHomeId(homeId);
        }

        HomeUserEntity entity = new HomeUserEntity();
        entity.setHome(home);
        entity.setUser(user);
        entity.setRoleInHome(roleInHome);
        entity.setAllowProfileActivation(allowProfileActivation);
        entity.setPrimary(isPrimary);

        homeUserRepository.save(entity);

        String provider = homeUserProviderResolver.resolveProviderName(user.getId());

        userEventOutboxService.enqueue(
                UserEventType.USER_ADDED_TO_HOME,
                user.getId(),
                buildEvent(
                        UserEventType.USER_ADDED_TO_HOME,
                        entity,
                        provider,
                        Map.of("action", "addUser")
                )
        );

        return homeUserViewAssembler.toItem(entity, provider);
    }

    @Transactional
    public HomeUserItemResponse updateUser(Long homeId, Long userId, UpdateHomeUserRequest request) {
        HomeUserEntity entity = homeUserRepository.findByHomeIdAndUserId(homeId, userId)
                .orElseThrow(() -> new BadRequestException("User is not a member of this home"));

        HomeUserRole oldRole = entity.getRoleInHome();
        boolean oldAllow = entity.isAllowProfileActivation();
        boolean oldPrimary = entity.isPrimary();

        HomeUserRole targetRole = request.roleInHome() != null
                ? request.roleInHome()
                : entity.getRoleInHome();

        boolean targetAllowProfileActivation = Optional.ofNullable(request.allowProfileActivation())
                .orElse(entity.isAllowProfileActivation());

        boolean targetPrimary = Optional.ofNullable(request.isPrimary())
                .orElse(entity.isPrimary());

        homeUserRoleChangePolicy.validateRoleChange(entity, request.roleInHome());
        homeUserPrimaryEligibilityPolicy.validate(targetRole, targetPrimary, targetAllowProfileActivation);
        homeUserPrimaryEligibilityPolicy.validatePrimaryActivationChange(entity, request.allowProfileActivation());

        if (request.roleInHome() != null) {
            if (request.roleInHome() == HomeUserRole.OWNER) {
                homeUserRoleChangePolicy.demoteExistingOwner(homeId, userId);
            }
            entity.setRoleInHome(request.roleInHome());
        }

        if (request.allowProfileActivation() != null) {
            entity.setAllowProfileActivation(request.allowProfileActivation());
        }

        if (request.isPrimary() != null) {
            if (request.isPrimary()) {
                homeUserRepository.clearPrimaryByHomeId(homeId);
                entity.setPrimary(true);
            } else {
                entity.setPrimary(false);
            }
        }

        String provider = homeUserProviderResolver.resolveProviderName(entity.getUser().getId());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", "updateUser");
        metadata.put("oldRoleInHome", oldRole != null ? oldRole.name() : null);
        metadata.put("newRoleInHome", entity.getRoleInHome() != null ? entity.getRoleInHome().name() : null);
        metadata.put("oldAllowProfileActivation", oldAllow);
        metadata.put("newAllowProfileActivation", entity.isAllowProfileActivation());
        metadata.put("oldPrimary", oldPrimary);
        metadata.put("newPrimary", entity.isPrimary());

        userEventOutboxService.enqueue(
                UserEventType.USER_UPDATED_IN_HOME,
                entity.getUser().getId(),
                buildEvent(
                        UserEventType.USER_UPDATED_IN_HOME,
                        entity,
                        provider,
                        metadata
                )
        );

        return homeUserViewAssembler.toItem(entity, provider);
    }

    @Transactional
    public void removeUser(Long homeId, Long userId) {
        HomeUserEntity entity = homeUserRepository.findByHomeIdAndUserId(homeId, userId)
                .orElseThrow(() -> new BadRequestException("User is not a member of this home"));

        homeUserRoleChangePolicy.validateRemovable(entity);

        String provider = homeUserProviderResolver.resolveProviderName(entity.getUser().getId());

        userEventOutboxService.enqueue(
                UserEventType.USER_REMOVED_FROM_HOME,
                entity.getUser().getId(),
                buildEvent(
                        UserEventType.USER_REMOVED_FROM_HOME,
                        entity,
                        provider,
                        Map.of("action", "removeUser")
                )
        );

        homeUserRepository.delete(entity);
    }

    @Transactional
    public void setPassword(Long homeId, Long userId, SetHomeUserPasswordRequest request) {
        if (!homeUserRepository.existsByHomeIdAndUserId(homeId, userId)) {
            throw new BadRequestException("User is not a member of this home");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        boolean hasLocalProvider = userAuthProviderRepository.existsByUserIdAndProvider(userId, AuthProvider.LOCAL);
        if (!hasLocalProvider) {
            throw new BadRequestException("This user does not use a LOCAL account and cannot have their password set");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(Boolean.TRUE.equals(request.requireChangeOnNextLogin()));

        HomeUserEntity membership = homeUserRepository.findByHomeIdAndUserId(homeId, userId)
                .orElseThrow(() -> new BadRequestException("User is not a member of this home"));

        String provider = homeUserProviderResolver.resolveProviderName(userId);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", "setPassword");
        metadata.put("requireChangeOnNextLogin", Boolean.TRUE.equals(request.requireChangeOnNextLogin()));
        metadata.put("passwordChanged", true);

        userEventOutboxService.enqueue(
                UserEventType.USER_PASSWORD_SET,
                userId,
                buildEvent(
                        UserEventType.USER_PASSWORD_SET,
                        membership,
                        provider,
                        metadata
                )
        );
    }

    private void ensureHomeExists(Long homeId) {
        if (!homeRepository.existsById(homeId)) {
            throw new BadRequestException("Home not found");
        }
    }

    private UserOperationEvent buildEvent(
            String eventType,
            HomeUserEntity entity,
            String provider,
            Map<String, Object> metadata
    ) {
        return new UserOperationEvent(
                eventType,
                entity.getHome().getId(),
                entity.getUser().getId(),
                entity.getUser().getUsername(),
                entity.getRoleInHome() != null ? entity.getRoleInHome().name() : null,
                entity.isAllowProfileActivation(),
                entity.isPrimary(),
                provider,
                currentUserService.getCurrentUserId(),
                OffsetDateTime.now(),
                metadata
        );
    }
}