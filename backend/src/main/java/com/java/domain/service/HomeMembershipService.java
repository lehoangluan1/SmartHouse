package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.domain.HomeUserRole;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.entity.HomeUserEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.HomeUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeMembershipService {

    private final HomeUserRepository homeUserRepository;

    public HomeUserEntity assignPrimaryMembership(UserEntity user, HomeEntity home, HomeUserRole roleInHome, Boolean createNewHome) {
        homeUserRepository.clearPrimaryByUserId(user.getId());

        HomeUserEntity homeUser = new HomeUserEntity();
        homeUser.setHome(home);
        homeUser.setUser(user);
        homeUser.setRoleInHome(roleInHome);
        homeUser.setPrimary(Boolean.TRUE.equals(createNewHome));
        homeUser.setAllowProfileActivation(false);

        return homeUserRepository.save(homeUser);
    }
}