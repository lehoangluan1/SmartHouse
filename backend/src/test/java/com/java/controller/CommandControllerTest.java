package com.java.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.java.config.ApiResponse;
import com.java.controller.dto.NextCommandResponse;
import com.java.domain.service.CommandLongPollNotifier;
import com.java.domain.service.ControlCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class CommandControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void nextBatchReturnsMapWithoutWaitingWhenCommandExists() {
        ControlCommandService service = mock(ControlCommandService.class);
        CommandLongPollNotifier notifier = new CommandLongPollNotifier();
        CommandController controller = new CommandController(service, notifier);

        NextCommandResponse fan = new NextCommandResponse(11L, 1L, "ohstem-fan-ctrl-01", "power", "on", "manual");
        Map<String, NextCommandResponse> commands = new LinkedHashMap<>();
        commands.put("ohstem-fan-ctrl-01", fan);
        commands.put("ohstem-light-ctrl-01", null);

        when(service.clampWaitMs(1500L)).thenReturn(1500L);
        when(service.getNextCommandsImmediate(List.of("ohstem-fan-ctrl-01", "ohstem-light-ctrl-01")))
                .thenReturn(commands);

        var deferred = controller.nextBatch("ohstem-fan-ctrl-01,ohstem-light-ctrl-01", 1500L);
        ResponseEntity<ApiResponse<Map<String, NextCommandResponse>>> response =
                (ResponseEntity<ApiResponse<Map<String, NextCommandResponse>>>) deferred.getResult();

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).containsEntry("ohstem-fan-ctrl-01", fan);
        notifier.shutdown();
    }
}
