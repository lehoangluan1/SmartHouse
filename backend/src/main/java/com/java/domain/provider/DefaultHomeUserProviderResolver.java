package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.UserAuthProviderEntity;
import com.java.persistence.repo.UserAuthProviderRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultHomeUserProviderResolver implements HomeUserProviderResolver {

    private final UserAuthProviderRepository userAuthProviderRepository;

    @Override
    public String resolveProviderName(Long userId) {
        return userAuthProviderRepository.findFirstByUserIdOrderByLinkedAtAsc(userId)
                .map(UserAuthProviderEntity::getProvider)
                .map(Enum::name)
                .orElse("");
    }
}