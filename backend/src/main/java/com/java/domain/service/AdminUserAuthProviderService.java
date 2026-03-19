package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.LinkCurrentGoogleAccountRequest;
import com.java.controller.dto.LinkUserAuthProviderRequest;
import com.java.controller.dto.UserAuthProviderItemResponse;
import com.java.controller.dto.UserAuthProviderListResponse;
import com.java.domain.AuthProvider;
import com.java.domain.UserEventType;
import com.java.domain.provider.GoogleOAuthClient;
import com.java.domain.service.dto.GoogleUserInfo;
import com.java.persistence.entity.UserAuthProviderEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.UserAuthProviderRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserAuthProviderService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final CurrentUserService currentUserService;
    private final GoogleOAuthClient googleOAuthClient;
    private final UserEventOutboxService userEventOutboxService;

    @Transactional(readOnly = true)
    public UserAuthProviderListResponse getProviders(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        List<UserAuthProviderItemResponse> providers = userAuthProviderRepository
                .findAllByUserIdOrderByLinkedAtAsc(userId)
                .stream()
                .map(this::toItem)
                .toList();

        return new UserAuthProviderListResponse(
                user.getId(),
                user.getUsername(),
                providers
        );
    }

    @Transactional(readOnly = true)
    public UserAuthProviderListResponse getCurrentUserProviders() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return getProviders(currentUserId);
    }

    @Transactional
    public UserAuthProviderListResponse linkProvider(Long userId, LinkUserAuthProviderRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        AuthProvider provider = request.provider();
        if (provider == null) {
            throw new BadRequestException("Provider cannot be empty");
        }

        if (provider == AuthProvider.LOCAL) {
            throw new BadRequestException("LOCAL login uses existing username/password, no need to link additional provider");
        }

        String providerEmail = normalizeEmail(request.providerEmail());
        if (providerEmail.isBlank()) {
            throw new BadRequestException("Provider email cannot be empty");
        }

        if (userAuthProviderRepository.existsByUserIdAndProvider(userId, provider)) {
            throw new BadRequestException("User has already linked this provider");
        }

        userAuthProviderRepository.findByProviderAndProviderEmail(provider, providerEmail)
                .ifPresent(existing -> {
                    if (!existing.getUser().getId().equals(userId)) {
                        throw new BadRequestException("This Google account has already been linked to another user");
                    }
                });

        UserAuthProviderEntity entity = new UserAuthProviderEntity();
        entity.setUser(user);
        entity.setProvider(provider);
        entity.setProviderEmail(providerEmail);
        entity.setLinkedAt(OffsetDateTime.now());

        userAuthProviderRepository.save(entity);

        publishLinkEventIfNeeded(user, provider, providerEmail, entity.getLinkedAt());

        return getProviders(userId);
    }

    @Transactional
    public UserAuthProviderListResponse linkGoogleForCurrentUser(LinkCurrentGoogleAccountRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();

        GoogleUserInfo googleUser = googleOAuthClient.exchangeAndFetchUser(
                request.authorizationCode(),
                request.redirectUri()
        );

        String providerEmail = normalizeEmail(googleUser.email());
        if (providerEmail.isBlank()) {
            throw new BadRequestException("Google did not return a valid email");
        }

        userAuthProviderRepository.findByProviderAndProviderEmail(AuthProvider.GOOGLE, providerEmail)
                .ifPresent(existing -> {
                    if (!existing.getUser().getId().equals(currentUserId)) {
                        throw new BadRequestException("This Google account has already been linked to another user");
                    }
                });

        if (!userAuthProviderRepository.existsByUserIdAndProvider(currentUserId, AuthProvider.GOOGLE)) {
            LinkUserAuthProviderRequest linkRequest = new LinkUserAuthProviderRequest(
                    AuthProvider.GOOGLE,
                    providerEmail
            );
            return linkProvider(currentUserId, linkRequest);
        }

        return getProviders(currentUserId);
    }

    private void publishLinkEventIfNeeded(
            UserEntity user,
            AuthProvider provider,
            String providerEmail,
            OffsetDateTime linkedAt
    ) {
        if (provider != AuthProvider.GOOGLE) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user.getId());
        payload.put("username", user.getUsername());
        payload.put("email", providerEmail);
        payload.put("provider", provider.name());
        payload.put("providerEmail", providerEmail);
        payload.put("linkedAt", linkedAt);

        userEventOutboxService.enqueue(
                UserEventType.USER_GOOGLE_LINKED,
                user.getId(),
                payload
        );
    }

    private UserAuthProviderItemResponse toItem(UserAuthProviderEntity entity) {
        return new UserAuthProviderItemResponse(
                entity.getProvider().name(),
                entity.getProviderEmail(),
                entity.getLinkedAt()
        );
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}