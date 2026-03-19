package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.controller.dto.HomeUserItemResponse;
import com.java.persistence.entity.HomeUserEntity;
import com.java.persistence.entity.UserEntity;

@Component
public class DefaultHomeUserViewAssembler implements HomeUserViewAssembler {

    @Override
    public HomeUserItemResponse toItem(HomeUserEntity entity, String provider) {
        UserEntity user = entity.getUser();

        return new HomeUserItemResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                entity.getRoleInHome().name(),
                user.getStatus().name(),
                provider,
                entity.isAllowProfileActivation(),
                user.isMustChangePassword(),
                entity.isPrimary()
        );
    }
}