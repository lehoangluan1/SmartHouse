package com.java;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.java.config.GoogleOAuthProperties;
import com.java.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    JwtProperties.class,
    GoogleOAuthProperties.class,
})
@EnableScheduling
public class SmartHouseApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartHouseApplication.class, args);
    }
}