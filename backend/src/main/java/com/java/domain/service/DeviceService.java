package com.java.domain.service;

import com.java.config.BadRequestException;
import com.java.config.NotFoundException;
import com.java.controller.dto.DeviceCreateRequest;
import com.java.controller.dto.DeviceResponse;
import com.java.controller.dto.DeviceStateResponse;
import com.java.mapper.DeviceMapper;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.entity.UserEntity;
import com.java.persistence.repo.DeviceRepository;
import com.java.persistence.repo.HomeRepository;
import com.java.persistence.repo.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final HomeRepository homeRepository;
    private final UserRepository userRepository;

    private final DeviceCreateValidator deviceCreateValidator;
    private final DeviceMapper deviceMapper;

    public List<DeviceResponse> getByHome(Long homeId) {
        return deviceRepository.findByHomeId(homeId)
                .stream()
                .map(deviceMapper::toDeviceResponse)
                .toList();
    }

    public DeviceResponse getById(Long id) {
        DeviceEntity device = findDeviceOrThrow(id);
        return deviceMapper.toDeviceResponse(device);
    }

    public DeviceEntity getByDeviceKey(String deviceKey) {
        return deviceRepository.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new NotFoundException("Device key not found"));
    }

    public DeviceStateResponse getState(Long deviceId) {
        DeviceEntity device = findDeviceOrThrow(deviceId);
        return deviceMapper.toDeviceStateResponse(device);
    }

    @Transactional
    public DeviceResponse createForHome(Long homeId, Long userId, DeviceCreateRequest request) {
        deviceCreateValidator.validate(homeId, userId, request);

        String deviceKey = request.deviceKey().trim();
        if (deviceRepository.findByDeviceKey(deviceKey).isPresent()) {
            throw new BadRequestException("Device key already exists");
        }

        HomeEntity home = homeRepository.findById(homeId)
                .orElseThrow(() -> new NotFoundException("Home not found"));

        UserEntity installedBy = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        DeviceEntity device = deviceMapper.toNewEntity(request, home, installedBy);
        DeviceEntity saved = deviceRepository.save(device);
        return deviceMapper.toDeviceResponse(saved);
    }

    private DeviceEntity findDeviceOrThrow(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Device not found"));
    }
}