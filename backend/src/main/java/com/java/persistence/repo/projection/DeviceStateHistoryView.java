package com.java.persistence.repo.projection;

import java.time.OffsetDateTime;

public interface DeviceStateHistoryView {
    Long getId();
    String getCapabilityCode();
    Boolean getValueBoolean();
    Double getValueNumber();
    String getValueText();
    OffsetDateTime getCreatedAt();
}