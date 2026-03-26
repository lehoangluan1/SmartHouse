package com.java.eventing;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.java.domain.service.ModeAutomationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeModeAutomationListener {

    private final ModeAutomationService modeAutomationService;

    @EventListener
    public void onEvent(HomeModeChangedEvent event) {
        if (event == null || event.getHomeId() == null) {
            return;
        }

        log.debug("HomeModeAutomationListener: triggering automation for homeId={}, mode={}",
                event.getHomeId(),
                event.getMode());

        modeAutomationService.evaluateAllByHome(event.getHomeId());
    }
}