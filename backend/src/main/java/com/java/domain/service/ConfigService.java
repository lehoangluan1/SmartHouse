package com.java.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.ConfigResponse;
import com.java.controller.dto.ConfigUpsertRequest;
import com.java.mapper.ConfigResponseMapper;
import com.java.mapper.ConfigSnapshotFactory;
import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.repo.ConfigRepository;
import com.java.persistence.repo.HomeRepository;
import com.java.persistence.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfigService {

    private final ConfigRepository configRepository;
    private final HomeRepository homeRepository;
    private final UserRepository userRepository;

    private final ConfigValidator configValidator;
    private final ConfigUpdater configUpdater;
    private final ConfigActivityLogger configActivityLogger;
    private final HomeAccessGuard homeAccessGuard;
    private final ConfigResponseMapper configResponseMapper;
    private final ConfigSnapshotFactory configSnapshotFactory;

    public List<ConfigResponse> getByHome(Long homeId) {
        return configRepository.findByHomeIdOrderByUpdatedAtDesc(homeId)
                .stream()
                .map(configResponseMapper::toResponse)
                .toList();
    }

    public ConfigResponse getActiveByHome(Long homeId) {
        homeAccessGuard.requireHomeMembership(homeId);

        return configRepository.findFirstByHomeIdAndIsActiveTrue(homeId)
                .map(configResponseMapper::toResponse)
                .orElseGet(() -> configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(homeId)
                        .map(configResponseMapper::toResponse)
                        .orElse(null));
    }

    @Transactional
    public ConfigResponse upsert(Long homeId, Long configId, ConfigUpsertRequest request) {
        Long userId = homeAccessGuard.requireActivatedCurrentUserId(homeId);

        configValidator.validate(homeId, request);

        ConfigEntity entity = configId == null
                ? new ConfigEntity()
                : configRepository.findById(configId)
                        .filter(it -> it.getHome() != null && homeId.equals(it.getHome().getId()))
                        .orElseThrow(() -> new BadRequestException("Config does not exist"));

        boolean isNew = entity.getId() == null;
        ConfigEntity before = !isNew ? configSnapshotFactory.copy(entity) : null;

        configUpdater.apply(entity, homeRepository.getReferenceById(homeId), request);

        if (isNew) {
            entity.setCreatedBy(userRepository.getReferenceById(userId));

            boolean hasActive = configRepository.findFirstByHomeIdAndIsActiveTrue(homeId).isPresent();
            entity.setIsActive(!hasActive);
        }

        ConfigEntity saved = configRepository.save(entity);

        if (isNew) {
            configActivityLogger.logCreated(homeId, userId, saved);
        } else {
            configActivityLogger.logUpdated(homeId, userId, before, saved);
        }

        return configResponseMapper.toResponse(saved);
    }

    @Transactional
    public ConfigResponse activate(Long homeId, Long configId) {
        Long userId = homeAccessGuard.requireActivatedCurrentUserId(homeId);

        ConfigEntity target = configRepository.findById(configId)
                .filter(it -> it.getHome() != null && homeId.equals(it.getHome().getId()))
                .orElseThrow(() -> new BadRequestException("Config does not exist"));

        ConfigEntity currentActive = configRepository.findFirstByHomeIdAndIsActiveTrue(homeId)
                .orElse(null);

        if (currentActive != null && currentActive.getId().equals(target.getId())) {
            return configResponseMapper.toResponse(target);
        }

        ConfigEntity previousActive = currentActive != null
                ? configSnapshotFactory.copy(currentActive)
                : null;

        configRepository.deactivateAllByHomeId(homeId);
        target.setIsActive(true);

        ConfigEntity saved = configRepository.save(target);
        configActivityLogger.logActivated(homeId, userId, previousActive, saved);

        return configResponseMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long homeId, Long configId) {
        Long userId = homeAccessGuard.requireActivatedCurrentUserId(homeId);

        ConfigEntity target = configRepository.findById(configId)
                .filter(it -> it.getHome() != null && homeId.equals(it.getHome().getId()))
                .orElseThrow(() -> new BadRequestException("Config does not exist"));

        boolean wasActive = Boolean.TRUE.equals(target.getIsActive());
        ConfigEntity deletedSnapshot = configSnapshotFactory.copy(target);

        configRepository.delete(target);
        configActivityLogger.logDeleted(homeId, userId, deletedSnapshot, wasActive);

        if (wasActive) {
            configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(homeId)
                    .ifPresent(next -> {
                        configRepository.deactivateAllByHomeId(homeId);
                        next.setIsActive(true);
                        ConfigEntity saved = configRepository.save(next);
                        configActivityLogger.logAutoActivatedAfterDelete(homeId, null, saved);
                    });
        }
    }
}