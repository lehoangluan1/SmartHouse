package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.java.domain.AuthProvider;
import com.java.persistence.entity.UserAuthProviderEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.UserAuthProviderRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAuthProviderLinker {

    private final UserAuthProviderRepository userAuthProviderRepository;

    public void link(UserEntity user, AuthProvider provider) {
        UserAuthProviderEntity authProvider = new UserAuthProviderEntity();
        authProvider.setUser(user);
        authProvider.setProvider(provider);
        authProvider.setProviderUserId(user.getUsername());
        authProvider.setProviderEmail(user.getUsername());
        authProvider.setLinkedAt(OffsetDateTime.now());

        userAuthProviderRepository.save(authProvider);
    }
}