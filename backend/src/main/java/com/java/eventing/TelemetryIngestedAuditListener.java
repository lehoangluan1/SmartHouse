package com.java.eventing;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.java.domain.service.TelemetryAuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryIngestedAuditListener {

    private final TelemetryAuditService telemetryAuditService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvent(TelemetryIngestedEvent event) {
        if (event == null || event.getHomeId() == null || event.getDeviceId() == null) {
            return;
        }

        log.debug(
                "TelemetryIngestedAuditListener received: homeId={}, deviceId={}, value={}, createdAt={}",
                event.getHomeId(),
                event.getDeviceId(),
                event.getRawValue(),
                event.getCreatedAt()
        );

        if (event.isChanged()) {
            telemetryAuditService.logIngest(event);
        } else {
            telemetryAuditService.logIngestWithoutStateChange(event);
        }
    }
}