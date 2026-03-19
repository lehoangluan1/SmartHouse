package com.java.domain.provider;

import com.java.persistence.entity.ActivityLogEntity;

public interface ActivityStatusResolver {
    String resolve(ActivityLogEntity entity);
}
