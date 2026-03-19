package com.java.domain.provider;

import com.java.persistence.entity.ActivityLogEntity;

public interface ConfigLikeActivityDecider {
    boolean isConfigLike(ActivityLogEntity entity);
}