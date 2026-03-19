package com.java.domain.provider;

import com.java.domain.service.dto.AuthenticationCommand;
import com.java.domain.AuthProvider;
import com.java.domain.service.dto.AuthenticatedUserPrincipal;

public interface AuthenticationStrategy {
    boolean supports(AuthProvider provider);
    AuthenticatedUserPrincipal authenticate(AuthenticationCommand command);
}