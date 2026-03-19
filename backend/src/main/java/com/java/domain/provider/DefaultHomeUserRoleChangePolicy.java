package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.domain.HomeUserRole;
import com.java.persistence.entity.HomeUserEntity;
import com.java.persistence.repo.HomeUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultHomeUserRoleChangePolicy implements HomeUserRoleChangePolicy {

    private final HomeUserRepository homeUserRepository;

    @Override
    public void validateRoleChange(HomeUserEntity entity, HomeUserRole requestedRole) {
        if (requestedRole == null) {
            return;
        }

        if (entity.getRoleInHome() == HomeUserRole.OWNER && requestedRole != HomeUserRole.OWNER) {
            throw new BadRequestException("Cannot change OWNER to another role directly. Please designate a new OWNER first.");
        }
    }

    @Override
    public void validateRemovable(HomeUserEntity entity) {
        if (entity.getRoleInHome() == HomeUserRole.OWNER) {
            throw new BadRequestException("Cannot remove OWNER from home. Please transfer OWNER rights to someone else first.");
        }

        if (entity.isPrimary()) {
            throw new BadRequestException("Cannot remove membership that is the primary profile");
        }
    }

    @Override
    public void demoteExistingOwner(Long homeId, Long newOwnerUserId) {
        homeUserRepository.findAllByHomeId(homeId).stream()
                .filter(item -> item.getRoleInHome() == HomeUserRole.OWNER)
                .filter(item -> !item.getUser().getId().equals(newOwnerUserId))
                .forEach(item -> item.setRoleInHome(HomeUserRole.CO_OWNER));
    }
}