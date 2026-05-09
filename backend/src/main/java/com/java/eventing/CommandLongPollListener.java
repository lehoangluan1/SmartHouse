package com.java.eventing;

import org.springframework.stereotype.Component;

import com.java.domain.events.ControlCommandEvent;
import com.java.domain.service.CommandLongPollNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandLongPollListener implements DomainEventListener<ControlCommandEvent> {

    private final CommandLongPollNotifier commandLongPollNotifier;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof ControlCommandEvent;
    }

    @Override
    public void onEvent(ControlCommandEvent event) {
        if (event == null || event.deviceKey() == null) {
            return;
        }

        int waiters = commandLongPollNotifier.signal(event.deviceKey());
        if (waiters > 0) {
            log.debug("COMMAND long-poll signalled id={} waiters={}", event.commandId(), waiters);
        } else {
            log.debug("COMMAND long-poll signalled id={} waiters=0", event.commandId());
        }
    }
}
