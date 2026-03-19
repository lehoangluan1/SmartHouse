package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.CreateUserRequest;
import com.java.controller.dto.UserProvisionResponse;
import com.java.domain.UserEventType;
import com.java.domain.service.ProvisionedUserFactory.ProvisionedUser;
import com.java.domain.service.dto.HomeAssignmentResult;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final UsernameNormalizer usernameNormalizer;
    private final UserProvisionRequestValidator validator;
    private final ProvisionedUserFactory provisionedUserFactory;
    private final UserAuthProviderLinker userAuthProviderLinker;
    private final HomeAssignmentService homeAssignmentService;
    private final HomeMembershipService homeMembershipService;
    private final UserProvisionEventFactory userProvisionEventFactory;
    private final UserEventOutboxService userEventOutboxService;

    @Transactional
    public UserProvisionResponse createUser(CreateUserRequest request) {
        validator.validate(request);

        String username = usernameNormalizer.normalize(request.username());
        if (username.isBlank()) {
            throw new BadRequestException("Username must not be blank");
        }

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already exists");
        }

        ProvisionedUser provisionedUser = provisionedUserFactory.create(request, username);

        UserEntity savedUser = userRepository.save(provisionedUser.user());
        userAuthProviderLinker.link(savedUser, request.provider());

        HomeAssignmentResult assignment = homeAssignmentService.assign(request, savedUser);
        homeMembershipService.assignPrimaryMembership(savedUser, assignment.home(), assignment.roleInHome(), assignment.createdNewHome());

        var event = userProvisionEventFactory.buildProvisionedEvent(
                request,
                savedUser,
                assignment.home(),
                assignment.roleInHome(),
                provisionedUser.temporaryPassword(),
                assignment.createdNewHome()
        );

        userEventOutboxService.enqueue(
                UserEventType.USER_PROVISIONED,
                savedUser.getId(),
                event
        );

        return new UserProvisionResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                request.provider(),
                savedUser.getRole(),
                assignment.roleInHome(),
                savedUser.getStatus(),
                savedUser.isMustChangePassword(),
                true,
                assignment.home().getId(),
                assignment.home().getName(),
                assignment.home().getAddress()
        );
    }
}