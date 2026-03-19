package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.java.controller.dto.CreateUserRequest;
import com.java.domain.HomeUserRole;
import com.java.domain.UserEventType;
import com.java.domain.service.dto.UserOperationEvent;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserProvisionEventFactory {

    private final CurrentUserService currentUserService;

    public UserOperationEvent buildProvisionedEvent(
            CreateUserRequest request,
            UserEntity user,
            HomeEntity home,
            HomeUserRole roleInHome,
            String temporaryPassword,
            boolean createdNewHome
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", "createUser");
        metadata.put("homeAssignmentMode", enumName(request.homeAssignmentMode()));
        metadata.put("systemRole", enumName(request.systemRole()));
        metadata.put("homeRole", enumName(roleInHome));
        metadata.put("provider", enumName(request.provider()));
        metadata.put("temporaryPassword", temporaryPassword);
        metadata.put("mustChangePassword", user.isMustChangePassword());
        metadata.put("createdNewHome", createdNewHome);
        metadata.put("homeName", home.getName());
        metadata.put("homeAddress", home.getAddress());

        return new UserOperationEvent(
                UserEventType.USER_PROVISIONED,
                home.getId(),
                user.getId(),
                user.getUsername(),
                enumName(roleInHome),
                false,
                true,
                enumName(request.provider()),
                currentUserService.getCurrentUserId(),
                OffsetDateTime.now(),
                metadata
        );
    }

    private String enumName(Enum<?> value) {
        return Optional.ofNullable(value).map(Enum::name).orElse(null);
    }
}