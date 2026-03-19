package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.domain.UserStatus;
import com.java.persistence.entity.UserEntity;

@Component
public class UserLoginPolicy {

    public void ensureLoginAllowed(UserEntity user) {
        if (user == null) {
            throw new BadRequestException("User does not exist");
        }

        if (user.getStatus() == null) {
            throw new BadRequestException("Account status is invalid");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is not allowed to login");
        }
    }
}