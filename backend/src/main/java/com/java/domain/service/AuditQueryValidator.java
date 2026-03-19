package com.java.domain.service;
import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.domain.service.dto.AuditQuery;

@Component
public class AuditQueryValidator {

    public void validate(AuditQuery query) {
        if (query == null) {
            throw new BadRequestException("Invalid request");
        }
        if (query.homeId() == null) {
            throw new BadRequestException("homeId cannot be empty");
        }
        if (query.from() == null || query.to() == null) {
            throw new BadRequestException("from/to cannot be empty");
        }
        if (query.from().isAfter(query.to())) {
            throw new BadRequestException("Invalid time: from must be before or equal to to");
        }
        if (query.configPage() < 0 || query.eventPage() < 0) {
            throw new BadRequestException("page cannot be less than 0");
        }
        if (query.configSize() <= 0 || query.eventSize() <= 0) {
            throw new BadRequestException("size must be greater than 0");
        }
    }
}