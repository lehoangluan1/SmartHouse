package com.java.domain.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.java.domain.service.dto.AuthenticatedUserPrincipal;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
            return authenticatedUserPrincipal;
        }

        if (principal instanceof UserDetails userDetails) {
            return buildPrincipalFromUsername(userDetails.getUsername());
        }

        if (principal instanceof String username && !"anonymousUser".equals(username)) {
            return buildPrincipalFromUsername(username);
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    public Long getCurrentUserId() {
        return getCurrentPrincipal().id();
    }

    public String getCurrentUsername() {
        return getCurrentPrincipal().username();
    }

    private AuthenticatedUserPrincipal buildPrincipalFromUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        return new AuthenticatedUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                null,
                user.isMustChangePassword(),
                user.getStatus().name()
        );
    }
}