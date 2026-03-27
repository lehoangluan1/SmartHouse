package com.java.domain.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.java.domain.AlertType;
import com.java.persistence.entity.AlertEntity;

@Component
public class TelegramAlertPolicy {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlertPolicy.class);

    private static final Set<AlertType> TELEGRAM_TYPES = EnumSet.of(
            AlertType.DEVICE_OFFLINE,
            AlertType.SENSOR_ERROR,
            AlertType.WRONG_PASSWORD,
            AlertType.HIGH_TEMPERATURE,
            AlertType.LOW_TEMPERATURE,
            AlertType.HIGH_HUMIDITY,
            AlertType.LOW_HUMIDITY,
            AlertType.HIGH_LIGHT,
            AlertType.LOW_LIGHT,
            AlertType.MOTION_DETECTED,
            AlertType.CRITICAL_TEMP
    );

    private static final Duration RE_NOTIFY_COOLDOWN = Duration.ofMinutes(2);

    public boolean shouldNotifyOnNewActive(AlertType type) {
        boolean result = type != null && TELEGRAM_TYPES.contains(type);
        log.info("Telegram policy new active: type={}, shouldNotify={}", type, result);
        return result;
    }

    public boolean shouldNotifyOnRefresh(AlertEntity alert, AlertType type, OffsetDateTime now) {
        if (type == null || alert == null || !TELEGRAM_TYPES.contains(type)) {
            return false;
        }

        OffsetDateTime baseTime = alert.getLastNotifiedAt();

        if (baseTime == null) {
            log.info("Telegram policy refresh: type={}, alertId={}, lastNotifiedAt=null, now={}, shouldNotify=true",
                    type, alert.getId(), now);
            return true;
        }

        boolean result = !baseTime.plus(RE_NOTIFY_COOLDOWN).isAfter(now);

        log.info(
                "Telegram policy refresh: type={}, alertId={}, lastNotifiedAt={}, now={}, cooldown={}, shouldNotify={}",
                type,
                alert.getId(),
                baseTime,
                now,
                RE_NOTIFY_COOLDOWN,
                result
        );

        return result;
    }
}