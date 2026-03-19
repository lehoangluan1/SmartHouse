package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.config.BadRequestException;
import com.java.config.ForbiddenException;
import com.java.persistence.entity.HomeUserEntity;
import com.java.persistence.repo.HomeUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeAccessGuard {

    private final CurrentUserService currentUserService;
    private final HomeUserRepository homeUserRepository;

    public HomeUserEntity requireHomeMembership(Long homeId) {
        Long userId = currentUserService.getCurrentUserId();

        return homeUserRepository.findByHomeIdAndUserId(homeId, userId)
                .orElseThrow(() -> new ForbiddenException("You do not belong to this home"));
    }

    public HomeUserEntity requireActivatedProfile(Long homeId) {
        HomeUserEntity homeUser = requireHomeMembership(homeId);

        if (!homeUser.isAllowProfileActivation()) {
            throw new BadRequestException("Account is not authorized to perform operations on this home");
        }

        return homeUser;
    }

    public Long requireActivatedCurrentUserId(Long homeId) {
        return requireActivatedProfile(homeId).getUser().getId();
    }

    public String requireActivatedCurrentUsername(Long homeId) {
        return requireActivatedProfile(homeId).getUser().getUsername();
    }
}