package com.java.domain.provider;

import com.java.controller.dto.HomeUserItemResponse;
import com.java.persistence.entity.HomeUserEntity;

public interface HomeUserViewAssembler {
    HomeUserItemResponse toItem(HomeUserEntity entity, String provider);
}