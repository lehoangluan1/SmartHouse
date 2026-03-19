package com.java.domain.provider;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.ScheduleEntity;

@Component
public class ScheduleValueReader {

    public String readAsText(ScheduleEntity entity) {
        if (entity.getValueBoolean() != null) {
            return String.valueOf(entity.getValueBoolean());
        }
        if (entity.getValueNumber() != null) {
            return String.valueOf(entity.getValueNumber());
        }
        return entity.getValueText();
    }    
}
