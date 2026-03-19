package com.java.domain.provider;

import com.java.domain.HomeUserRole;
import com.java.persistence.entity.HomeUserEntity;

public interface HomeUserRoleChangePolicy {
    void validateRoleChange(HomeUserEntity entity, HomeUserRole requestedRole);
    void validateRemovable(HomeUserEntity entity);
    void demoteExistingOwner(Long homeId, Long newOwnerUserId);
}