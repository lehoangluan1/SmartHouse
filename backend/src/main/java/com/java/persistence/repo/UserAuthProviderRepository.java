package com.java.persistence.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.domain.AuthProvider;
import com.java.persistence.entity.UserAuthProviderEntity;

public interface UserAuthProviderRepository extends JpaRepository<UserAuthProviderEntity, Long> {

    Optional<UserAuthProviderEntity> findByUserIdAndProvider(Long userId, AuthProvider provider);

    Optional<UserAuthProviderEntity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<UserAuthProviderEntity> findByProviderAndProviderEmail(AuthProvider provider, String providerEmail);

    boolean existsByUserIdAndProvider(Long userId, AuthProvider provider);

    Optional<UserAuthProviderEntity> findFirstByUserId(Long userId);

    Optional<UserAuthProviderEntity> findFirstByUserIdOrderByLinkedAtAsc(Long userId);

    boolean existsByProviderAndProviderEmail(AuthProvider provider, String providerEmail);

    List<UserAuthProviderEntity> findAllByUserIdOrderByLinkedAtAsc(Long userId);
}