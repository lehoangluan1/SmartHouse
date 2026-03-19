package com.java.domain.service;

import com.java.config.ForbiddenException;
import com.java.persistence.repo.HomeUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeAccessService {
    private final HomeUserRepository homeUserRepository;

    public void ensureAccess(Long homeId, Long userId) {
        if (!homeUserRepository.existsByHomeIdAndUserId(homeId, userId)) {
            throw new ForbiddenException("You do not have permission to access this home");
        }
    }
}
