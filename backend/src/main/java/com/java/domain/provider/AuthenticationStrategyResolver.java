package com.java.domain.provider;

import java.util.List;

import org.springframework.stereotype.Service;

import com.java.config.BadRequestException;
import com.java.domain.AuthProvider;

@Service
public class AuthenticationStrategyResolver {

    private final List<AuthenticationStrategy> strategies;

    public AuthenticationStrategyResolver(List<AuthenticationStrategy> strategies){
        this.strategies = strategies;
    }

    public AuthenticationStrategy resolve(AuthProvider provider) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(provider))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Login method is not supported"));
    }
}