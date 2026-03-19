package com.java.domain.provider;
import com.java.persistence.entity.HomeUserEntity;
import com.java.domain.HomeUserRole;

public interface HomeUserPrimaryEligibilityPolicy {
    void validate(HomeUserRole roleInHome, boolean isPrimary, boolean allowProfileActivation);
    void validatePrimaryActivationChange(HomeUserEntity entity, Boolean requestedAllowProfileActivation);
}