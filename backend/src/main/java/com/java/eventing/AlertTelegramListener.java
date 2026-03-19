package com.java.eventing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.java.domain.service.AlertMessageFormatter;
import com.java.domain.service.TelegramNotifier;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertTelegramListener implements DomainEventListener<AlertNotificationEvent> {

    private static final Logger log = LoggerFactory.getLogger(AlertTelegramListener.class);

    private final TelegramNotifier telegramNotifier;
    private final DeviceRepository deviceRepository;
    private final AlertMessageFormatter alertMessageFormatter;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof AlertNotificationEvent;
    }

    @Override
    public void onEvent(AlertNotificationEvent event) {
        log.info("AlertTelegramListener received event: alertId={}, type={}, newlyCreated={}",
                event == null ? null : event.getAlertId(),
                event == null ? null : event.getType(),
                event == null ? null : event.isNewlyCreated());

        if (event == null) {
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

    private String buildMessage(AlertNotificationEvent event, String deviceName) {
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