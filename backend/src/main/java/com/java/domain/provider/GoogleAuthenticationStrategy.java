package com.java.domain.provider;

import org.springframework.stereotype.Service;

import com.java.config.BadRequestException;
import com.java.domain.AuthProvider;
import com.java.domain.HomeUserRole;
import com.java.domain.service.dto.AuthenticationCommand;
import com.java.domain.service.dto.AuthenticatedUserPrincipal;
import com.java.domain.service.dto.GoogleUserInfo;
import com.java.persistence.entity.UserAuthProviderEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.HomeUserRepository;
import com.java.persistence.repo.UserAuthProviderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleAuthenticationStrategy implements AuthenticationStrategy {

    private final GoogleOAuthClient googleOAuthClient;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final UserLoginPolicy userLoginPolicy;
    private final HomeUserRepository homeUserRepository;

    @Override
    public boolean supports(AuthProvider provider) {
        return provider == AuthProvider.GOOGLE;
    }

    @Override
    public AuthenticatedUserPrincipal authenticate(AuthenticationCommand command) {
        GoogleUserInfo googleUser = googleOAuthClient.exchangeAndFetchUser(
                command.authorizationCode(),
                command.redirectUri()
        );

        UserEntity user = userAuthProviderRepository
                .findByProviderAndProviderEmail(AuthProvider.GOOGLE, googleUser.email())
                .map(UserAuthProviderEntity::getUser)
                .orElseThrow(() -> new BadRequestException("Account has not been granted permission"));

        userLoginPolicy.ensureLoginAllowed(user);
        HomeUserRole homeUserRole = homeUserRepository.findPrimaryHomeUserRoleIdByUserId(user.getId()).orElse(HomeUserRole.GUEST);

        return new AuthenticatedUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                homeUserRole,
                user.isMustChangePassword(),
                user.getStatus().name()
        );
    }
}