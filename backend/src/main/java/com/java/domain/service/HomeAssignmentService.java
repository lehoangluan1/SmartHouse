package com.java.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.java.config.BadRequestException;
import com.java.controller.dto.CreateUserRequest;
import com.java.controller.dto.HomeAssignmentMode;
import com.java.domain.HomeUserRole;
import com.java.domain.service.dto.HomeAssignmentResult;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.HomeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeAssignmentService {

    private final HomeRepository homeRepository;
    private final UsernameNormalizer normalizer;

    public HomeAssignmentResult assign(CreateUserRequest request, UserEntity user) {
        if (request.homeAssignmentMode() == HomeAssignmentMode.CREATE_NEW) {
            return createNewHome(request, user);
        }

        return joinExistingHome(request);
    }

    private HomeAssignmentResult createNewHome(CreateUserRequest request, UserEntity user) {
        HomeEntity home = new HomeEntity();
        home.setName(Optional.ofNullable(request.homeName()).map(String::trim).orElse(""));
        home.setAddress(normalizer.normalizeNullable(request.address()));
        home.setOwner(user);
    
        HomeEntity savedHome = homeRepository.save(home);
        return new HomeAssignmentResult(savedHome, HomeUserRole.OWNER, true);
    }

    private HomeAssignmentResult joinExistingHome(CreateUserRequest request) {
        HomeEntity home = homeRepository.findById(request.homeId())
                .orElseThrow(() -> new BadRequestException("Home not found"));

        return new HomeAssignmentResult(home, request.homeRole(), false);
    }
}