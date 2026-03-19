package com.java.domain.provider;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.domain.HomeUserRole;
import com.java.persistence.entity.HomeUserEntity;

@Component
public class DefaultHomeUserPrimaryEligibilityPolicy implements HomeUserPrimaryEligibilityPolicy {

    private static final Set<HomeUserRole> DISALLOWED_PRIMARY_ROLES = Set.of(
            HomeUserRole.GUEST,
            HomeUserRole.VIEWER,
            HomeUserRole.TECHNICIAN
    );

    @Override
    public void validate(HomeUserRole roleInHome, boolean isPrimary, boolean allowProfileActivation) {
        if (!isPrimary) {
            return;
        }

        if (DISALLOWED_PRIMARY_ROLES.contains(roleInHome)) {
            throw new BadRequestException("This role cannot be the primary profile");
        }

        if (!allowProfileActivation) {
            throw new BadRequestException("Can only set primary profile for membership that is allowed to activate");
        }
    }

    @Override
    public void validatePrimaryActivationChange(HomeUserEntity entity, Boolean requestedAllowProfileActivation) {
        if (requestedAllowProfileActivation == null) {
            return;
        }

        if (entity.isPrimary() && !requestedAllowProfileActivation) {
            throw new BadRequestException("Cannot disable profile activation of the primary profile. Please change the primary profile to another home first.");
        }
    }
}