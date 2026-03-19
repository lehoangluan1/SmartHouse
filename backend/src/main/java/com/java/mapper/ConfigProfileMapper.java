package com.java.mapper;

import org.springframework.stereotype.Component;

import com.java.domain.service.dto.ConfigProfileDto;
import com.java.persistence.entity.ConfigEntity;

@Component
public class ConfigProfileMapper {

    public ConfigProfileDto toDto(ConfigEntity cfg) {
        if (cfg == null) {
            return null;
        }

        return ConfigProfileDto.builder()
                .id(cfg.getId())
                .name(cfg.getName())
                .thigh(cfg.getThigh())
                .tlow(cfg.getTlow())
                .llow(cfg.getLlow())
                .lhigh(cfg.getLhigh())
                .tsleepHigh(cfg.getTsleepHigh())
                .tsleepLow(cfg.getTsleepLow())
                .tawayHigh(cfg.getTawayHigh())
                .tcritical(cfg.getTcritical())
                .nMinutes(cfg.getNMinutes())
                .mMinutes(cfg.getMMinutes())
                .tholdMinutes(cfg.getTholdMinutes())
                .build();
    }
}