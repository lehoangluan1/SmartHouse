package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.persistence.entity.UserEntity;

@Component
public class UserAccountPolicy {

    public void ensureLoginAllowed(UserEntity user) {
        if (user.getStatus() == null) {
            throw new BadRequestException("Account is invalid");
        }

        switch (user.getStatus()) {
            case ACTIVE -> {
            }
            case INACTIVE -> throw new BadRequestException("Account is disabled");
            case LOCKED -> throw new BadRequestException("Account is locked");
            default -> throw new BadRequestException("Account is inactive");
        }
    }
}