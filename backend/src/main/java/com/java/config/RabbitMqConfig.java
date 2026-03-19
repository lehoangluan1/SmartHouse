package com.java.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties({
        RabbitUserEventProperties.class,
        OutboxPublisherProperties.class,
        NotificationMailProperties.class
})
public class RabbitMqConfig {

    @PostConstruct
    public void init() {
        log.info("RabbitMqConfig initialized");
    }

    @Bean
    public DirectExchange userEventExchange(RabbitUserEventProperties properties) {
        log.info("Creating RabbitMQ exchange: name={}", properties.exchange());
        return new DirectExchange(properties.exchange(), true, false);
    }

    @Bean
    public Queue userEventQueue(RabbitUserEventProperties properties) {
        log.info("Creating RabbitMQ queue: name={}", properties.queue());
        return QueueBuilder.durable(properties.queue()).build();
    }

    @Bean
    public Binding userEventBinding(
            Queue userEventQueue,
            DirectExchange userEventExchange,
            RabbitUserEventProperties properties
    ) {
        log.info(
                "Binding RabbitMQ queue to exchange: queue={}, exchange={}, routingKey={}",
                userEventQueue.getName(),
                userEventExchange.getName(),
                properties.routingKey()
        );

        return BindingBuilder.bind(userEventQueue)
                .to(userEventExchange)
                .with(properties.routingKey());
    }
}