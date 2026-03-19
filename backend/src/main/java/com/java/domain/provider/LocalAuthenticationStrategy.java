package com.java.domain.provider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.java.config.BadRequestException;
import com.java.domain.AuthProvider;
import com.java.domain.HomeUserRole;
import com.java.domain.service.dto.AuthenticatedUserPrincipal;
import com.java.domain.service.dto.AuthenticationCommand;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.HomeUserRepository;
import com.java.persistence.repo.UserAuthProviderRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalAuthenticationStrategy implements AuthenticationStrategy {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLoginPolicy userLoginPolicy;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final HomeUserRepository homeUserRepository;

    @Override
    public boolean supports(AuthProvider provider) {
        return provider == AuthProvider.LOCAL;
    }

    @Override
    public AuthenticatedUserPrincipal authenticate(AuthenticationCommand command) {
        String username = normalize(command.username());

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));
        

        boolean hasLocalProvider = userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), AuthProvider.LOCAL);
        if (!hasLocalProvider) {
            throw new BadRequestException("This account has not linked password login");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BadRequestException("This account has not set up a password");
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid username or password");
        }

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
    
    private String normalize(String username) {
        return username == null ? "" : username.trim();
    }
}