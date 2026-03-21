package com.java.eventing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.java.domain.service.AlertMessageFormatter;
import com.java.domain.service.TelegramAlertPolicy;
import com.java.domain.service.TelegramNotifier;
import com.java.persistence.repo.AlertRepository;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertTelegramListener implements DomainEventListener<AlertActivatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AlertTelegramListener.class);

    private final TelegramNotifier telegramNotifier;
    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final AlertMessageFormatter alertMessageFormatter;
    private final TelegramAlertPolicy telegramAlertPolicy;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof AlertActivatedEvent;
    }

    @Override
    public void onEvent(AlertActivatedEvent event) {
        log.info("AlertTelegramListener received event: alertId={}, type={}, newlyCreated={}",
                event == null ? null : event.getAlertId(),
                event == null ? null : event.getType(),
                event == null ? null : event.isNewlyCreated());

        if (event == null || event.getType() == null) {
            return;
        }

        boolean shouldNotify;
        if (event.isNewlyCreated()) {
            shouldNotify = telegramAlertPolicy.shouldNotifyOnNewActive(event.getType());
        } else {
            shouldNotify = alertRepository.findById(event.getAlertId())
                    .map(alert -> telegramAlertPolicy.shouldNotifyOnRefresh(alert, event.getType(), event.getTriggeredAt()))
                    .orElse(false);
        }

        log.info("Telegram notify decision: alertId={}, type={}, shouldNotify={}",
                event.getAlertId(), event.getType(), shouldNotify);

        if (!shouldNotify) {
            return;
        }

        String deviceName = "Unknown device";
        if (event.getDeviceId() != null) {
            deviceName = deviceRepository.findById(event.getDeviceId())
                    .map(d -> d.getName())
                    .orElse("Unknown device");
        }

        String text = buildMessage(event, deviceName);
        log.info("Sending telegram for alertId={}", event.getAlertId());
        telegramNotifier.sendMessage(text);
    }

    private String buildMessage(AlertActivatedEvent event, String deviceName) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 SMART HOME ALERT\n");
        sb.append("Type: ").append(alertMessageFormatter.displayType(event.getType())).append("\n");
        sb.append("Device: ").append(deviceName).append("\n");
        sb.append("New: ").append(event.isNewlyCreated()).append("\n");

        if (event.getMessage() != null && !event.getMessage().isBlank()) {
            sb.append("Message: ").append(event.getMessage()).append("\n");
        }

        if (event.getTriggeredAt() != null) {
            sb.append("Time: ").append(event.getTriggeredAt()).append("\n");
        }

        return sb.toString();
    }
}