package com.java.domain.service;

import com.java.domain.SystemUserRole;

public interface AccessTokenIssuer {
    String issue(Long userId, String username, SystemUserRole role);
}