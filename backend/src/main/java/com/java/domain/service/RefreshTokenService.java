package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.domain.provider.RefreshTokenHasher;
import com.java.domain.provider.RefreshTokenIssuer;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.entity.UserRefreshTokenEntity;
import com.java.persistence.repo.UserRefreshTokenRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final RefreshTokenHasher refreshTokenHasher;

    @Value("${app.security.refresh-token.ttl-days:30}")
    private long refreshTokenTtlDays;

    @Transactional
    public IssuedRefreshToken issueForUser(Long userId, String createdByIp, String userAgent) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String rawToken = refreshTokenIssuer.issue();
        String tokenHash = refreshTokenHasher.hash(rawToken);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusDays(refreshTokenTtlDays);

        UserRefreshTokenEntity entity = new UserRefreshTokenEntity();
        entity.setUser(user);
        entity.setTokenHash(tokenHash);
        entity.setIssuedAt(now);
        entity.setExpiresAt(expiresAt);
        entity.setCreatedByIp(createdByIp);
        entity.setUserAgent(userAgent);

        userRefreshTokenRepository.save(entity);

        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public UserRefreshTokenEntity verifyUsableToken(String rawRefreshToken) {
        String tokenHash = refreshTokenHasher.hash(rawRefreshToken);

        UserRefreshTokenEntity token = userRefreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (token.getRevokedAt() != null) {
            throw new BadRequestException("Refresh token has been revoked");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Refresh token has expired");
        }

        return token;
    }

    @Transactional
    public RotatedRefreshToken rotate(String rawRefreshToken, String createdByIp, String userAgent) {
        UserRefreshTokenEntity currentToken = verifyUsableToken(rawRefreshToken);

        currentToken.setRevokedAt(OffsetDateTime.now());

        String newRawToken = refreshTokenIssuer.issue();
        String newTokenHash = refreshTokenHasher.hash(newRawToken);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusDays(refreshTokenTtlDays);

        UserRefreshTokenEntity replacement = new UserRefreshTokenEntity();
        replacement.setUser(currentToken.getUser());
        replacement.setTokenHash(newTokenHash);
        replacement.setIssuedAt(now);
        replacement.setExpiresAt(expiresAt);
        replacement.setCreatedByIp(createdByIp);
        replacement.setUserAgent(userAgent);

        userRefreshTokenRepository.save(replacement);

        return new RotatedRefreshToken(currentToken.getUser(), newRawToken, expiresAt);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        String tokenHash = refreshTokenHasher.hash(rawRefreshToken);

        userRefreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(OffsetDateTime.now());
                    }
                });
    }

    @Transactional
    public void revokeAllByUserId(Long userId) {
        List<UserRefreshTokenEntity> tokens = userRefreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        OffsetDateTime now = OffsetDateTime.now();

        for (UserRefreshTokenEntity token : tokens) {
            token.setRevokedAt(now);
        }
    }

    @Transactional
    public void deleteExpiredTokens() {
        userRefreshTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
    }

    public record IssuedRefreshToken(
            String rawToken,
            OffsetDateTime expiresAt
    ) {
    }

    public record RotatedRefreshToken(
            UserEntity user,
            String rawToken,
            OffsetDateTime expiresAt
    ) {
    }
}