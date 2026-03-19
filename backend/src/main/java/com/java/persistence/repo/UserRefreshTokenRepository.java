package com.java.persistence.repo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.persistence.entity.UserRefreshTokenEntity;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshTokenEntity, Long> {

    Optional<UserRefreshTokenEntity> findByTokenHash(String tokenHash);

    List<UserRefreshTokenEntity> findAllByUserIdAndRevokedAtIsNull(Long userId);

    void deleteByExpiresAtBefore(OffsetDateTime time);
}