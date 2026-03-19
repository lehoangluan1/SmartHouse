package com.java.adapter;

import com.java.persistence.entity.ControlCommandEntity;

public interface DeviceCommandAdapter {
    boolean supports(String deviceKey);
    AdapterResult send(ControlCommandEntity command);

    record AdapterResult(boolean success, String externalStatus, String message) {}
}