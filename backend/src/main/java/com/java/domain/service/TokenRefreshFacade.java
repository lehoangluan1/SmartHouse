package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.RefreshTokenResponse;
import com.java.domain.UserStatus;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.HomeUserRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenRefreshFacade {

    private final RefreshTokenService refreshTokenService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final UserRepository userRepository;
    private final HomeUserRepository homeUserRepository;

    @Transactional
    public RefreshTokenResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken, null, null);

        UserEntity user = userRepository.findById(rotated.user().getId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is inactive");
        }

        String accessToken = accessTokenIssuer.issue(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        Long homeId = homeUserRepository.findPrimaryHomeIdByUserId(user.getId()).orElse(null);

        return new RefreshTokenResponse(
                accessToken,
                rotated.rawToken(),
                homeId
        );
    }
}