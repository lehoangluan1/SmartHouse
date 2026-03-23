package com.java.domain.events;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.java.domain.service.TelegramNotifier;
import com.java.eventing.DomainEvent;
import com.java.eventing.DomainEventListener;
import com.java.persistence.repo.DeviceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertLifecycleTelegramListener implements DomainEventListener<AlertLifecycleEvent> {

    private static final Logger log = LoggerFactory.getLogger(AlertLifecycleTelegramListener.class);

    private final TelegramNotifier telegramNotifier;
    private final DeviceRepository deviceRepository;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof AlertLifecycleEvent;
    }

    @Override
    public void onEvent(AlertLifecycleEvent event) {
        log.info("Received lifecycle event: alertId={}, action={}",
                event.alertId(), event.action());

        String deviceName = "Unknown device";
        if (event.deviceId() != null) {
            deviceName = deviceRepository.findById(event.deviceId())
                    .map(d -> d.getName())
                    .orElse("Unknown device");
        }

        String text = buildMessage(event, deviceName);
        telegramNotifier.sendMessage(text);
    }

    private String buildMessage(AlertLifecycleEvent event, String deviceName) {
        return new StringBuilder()
                .append("📢 ALERT LIFECYCLE\n")
                .append("Action: ").append(event.action()).append("\n")
                .append("Device: ").append(deviceName).append("\n")
                .append("AlertId: ").append(event.alertId())
                .toString();
    }
}
