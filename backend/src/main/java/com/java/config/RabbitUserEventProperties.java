package com.java.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.user")
public record RabbitUserEventProperties(
        String exchange,
        String queue,
        String routingKey
) {
}