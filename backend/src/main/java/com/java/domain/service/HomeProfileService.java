package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.ActivateHomeProfileResponse;
import com.java.domain.UserEventType;
import com.java.domain.provider.HomeUserProviderResolver;
import com.java.domain.service.dto.UserOperationEvent;
import com.java.persistence.entity.HomeUserEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.HomeUserRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeProfileService {

    private final CurrentUserService currentUserService;
    private final HomeAccessService homeAccessService;
    private final HomeUserRepository homeUserRepository;
    private final UserRepository userRepository;
    private final HomeUserProviderResolver homeUserProviderResolver;
    private final UserEventOutboxService userEventOutboxService;

    @Transactional
    public ActivateHomeProfileResponse activate(Long homeId) {
        Long currentUserId = currentUserService.getCurrentUserId();

        homeAccessService.ensureAccess(homeId, currentUserId);

        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        HomeUserEntity membership = homeUserRepository.findByHomeIdAndUserId(homeId, currentUserId)
                .orElseThrow(() -> new BadRequestException("User is not a member of this home"));

        homeUserRepository.clearPrimaryByHomeId(homeId);
        membership.setPrimary(true);

        userEventOutboxService.enqueue(
                UserEventType.HOME_PROFILE_ACTIVATED,
                user.getId(),
                new UserOperationEvent(
                        UserEventType.HOME_PROFILE_ACTIVATED,
                        membership.getHome().getId(),
                        user.getId(),
                        user.getUsername(),
                        membership.getRoleInHome() != null ? membership.getRoleInHome().name() : null,
                        membership.isAllowProfileActivation(),
                        membership.isPrimary(),
                        homeUserProviderResolver.resolveProviderName(user.getId()),
                        currentUserId,
                        OffsetDateTime.now(),
                        Map.of("action", "activateHomeProfile")
                )
        );

        return new ActivateHomeProfileResponse(
                user.getId(),
                membership.getHome().getId(),
                membership.getRoleInHome().name(),
                membership.isAllowProfileActivation(),
                membership.isPrimary()
        );
    }
}