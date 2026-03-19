package com.java.domain.provider;

import com.java.persistence.entity.ActivityLogEntity;

public interface ActivityCategoryResolver {
    String resolve(ActivityLogEntity entity);
}