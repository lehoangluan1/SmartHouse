package com.java.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.java.domain.AlertType;

@Component
public class TelegramAlertPolicy {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlertPolicy.class);

    public boolean shouldNotify(AlertType type) {
        boolean result = type != null;
        log.info("Telegram policy check: type={}, shouldNotify={}", type, result);
        return result;
    }
}