package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.LoginResponse;
import com.java.domain.provider.AuthenticationStrategy;
import com.java.domain.provider.AuthenticationStrategyResolver;
import com.java.domain.service.dto.AuthenticatedUserPrincipal;
import com.java.domain.service.dto.AuthenticationCommand;
import com.java.persistence.repo.HomeUserRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationStrategyResolver resolver;
    private final AccessTokenIssuer accessTokenIssuer;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final HomeUserRepository homeUserRepository;

    @Transactional
    public LoginResponse login(AuthenticationCommand command) {
        AuthenticationStrategy strategy = resolver.resolve(command.provider());
        AuthenticatedUserPrincipal principal = strategy.authenticate(command);

        userRepository.findById(principal.id())
                .ifPresent(user -> user.setLastLogin(OffsetDateTime.now()));

        String accessToken = accessTokenIssuer.issue(
                principal.id(),
                principal.username(),
                principal.role()
        );

        RefreshTokenService.IssuedRefreshToken issuedRefreshToken =
                refreshTokenService.issueForUser(principal.id(), null, null);

        Long homeId = homeUserRepository.findPrimaryHomeIdByUserId(principal.id()).orElseThrow(() -> new BadRequestException("Cannot Access To Home"));

        return new LoginResponse(
                accessToken,
                issuedRefreshToken.rawToken(),
                principal.id(),
                principal.username(),
                principal.role().name(),
                principal.roleInHome().name(),
                principal.status(),
                principal.mustChangePassword(),
                homeId
        );
    }
}