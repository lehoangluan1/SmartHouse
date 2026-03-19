package com.java.domain.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigProfileDto {
    private Long id;
    private String name;
    private Double thigh;
    private Double tlow;
    private Integer llow;
    private Integer lhigh;
    private Double tsleepHigh;
    private Double tsleepLow;
    private Double tawayHigh;
    private Double tcritical;
    private Integer nMinutes;
    private Integer mMinutes;
    private Integer tholdMinutes;
}