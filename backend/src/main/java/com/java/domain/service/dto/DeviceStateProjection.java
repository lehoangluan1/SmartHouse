package com.java.domain.service.dto;


public interface DeviceStateProjection {

    void setFanStatus(String value);

    void setFanSpeed(Integer value);

    void setLightStatus(String value);

    void setLightLevel(Integer value);
}
