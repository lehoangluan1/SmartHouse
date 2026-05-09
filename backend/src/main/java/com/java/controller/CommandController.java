package com.java.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import com.java.config.ApiResponse;
import com.java.config.BadRequestException;
import com.java.controller.dto.CommandAckRequest;
import com.java.controller.dto.NextCommandResponse;
import com.java.domain.service.CommandLongPollNotifier;
import com.java.domain.service.ControlCommandService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/device")
@Slf4j
public class CommandController {

    private final ControlCommandService controlCommandService;
    private final CommandLongPollNotifier commandLongPollNotifier;

    public CommandController(
            ControlCommandService controlCommandService,
            CommandLongPollNotifier commandLongPollNotifier
    ) {
        this.controlCommandService = controlCommandService;
        this.commandLongPollNotifier = commandLongPollNotifier;
    }

    @GetMapping("/{deviceKey}/commands/next")
    public DeferredResult<ResponseEntity<ApiResponse<NextCommandResponse>>> next(
            @PathVariable String deviceKey,
            @RequestParam(required = false) Long waitMs
    ) {
        long startedAt = System.nanoTime();
        long effectiveWaitMs = controlCommandService.clampWaitMs(waitMs);
        DeferredResult<ResponseEntity<ApiResponse<NextCommandResponse>>> result =
                new DeferredResult<>(effectiveWaitMs + 250L, emptyCommandResponse());

        NextCommandResponse immediate = controlCommandService.getNextCommandImmediate(deviceKey);
        if (immediate != null || effectiveWaitMs <= 0) {
            completeNext(result, immediate, startedAt, deviceKey);
            return result;
        }

        commandLongPollNotifier.await(deviceKey, effectiveWaitMs)
                .whenComplete((ignored, ex) -> {
                    if (ex != null) {
                        log.warn("COMMAND long-poll wait failed key={} err={}", deviceKey, ex.getClass().getSimpleName());
                    }
                    NextCommandResponse cmd = controlCommandService.getNextCommandImmediate(deviceKey);
                    completeNext(result, cmd, startedAt, deviceKey);
                });

        return result;
    }

    @GetMapping("/commands/next-batch")
    public DeferredResult<ResponseEntity<ApiResponse<Map<String, NextCommandResponse>>>> nextBatch(
            @RequestParam String keys,
            @RequestParam(required = false) Long waitMs
    ) {
        long startedAt = System.nanoTime();
        List<String> deviceKeys = parseDeviceKeys(keys);
        long effectiveWaitMs = controlCommandService.clampWaitMs(waitMs);
        DeferredResult<ResponseEntity<ApiResponse<Map<String, NextCommandResponse>>>> result =
                new DeferredResult<>(effectiveWaitMs + 250L, batchResponse(emptyBatch(deviceKeys)));

        Map<String, NextCommandResponse> immediate = controlCommandService.getNextCommandsImmediate(deviceKeys);
        if (hasAnyCommand(immediate) || effectiveWaitMs <= 0) {
            completeBatch(result, immediate, startedAt, deviceKeys);
            return result;
        }

        commandLongPollNotifier.awaitAny(deviceKeys, effectiveWaitMs)
                .whenComplete((ignored, ex) -> {
                    if (ex != null) {
                        log.warn("COMMAND batch long-poll wait failed keys={} err={}", deviceKeys, ex.getClass().getSimpleName());
                    }
                    Map<String, NextCommandResponse> commands =
                            controlCommandService.getNextCommandsImmediate(deviceKeys);
                    completeBatch(result, commands, startedAt, deviceKeys);
                });

        return result;
    }

    @PostMapping("/{deviceKey}/commands/ack")
    public ResponseEntity<ApiResponse<Void>> ack(
            @PathVariable String deviceKey,
            @RequestBody CommandAckRequest req
    ) {
        if (req == null || req.id() == null) {
            throw new BadRequestException("id required");
        }

        controlCommandService.ackCommand(deviceKey, req.id());
        return ResponseEntity.ok(ApiResponse.ok(null, "Command acknowledged"));
    }

    private void completeNext(
            DeferredResult<ResponseEntity<ApiResponse<NextCommandResponse>>> result,
            NextCommandResponse cmd,
            long startedAt,
            String deviceKey
    ) {
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        log.debug("COMMAND next completed key={} id={} durationMs={}", deviceKey, cmd != null ? cmd.id() : null, elapsedMs);
        result.setResult(ResponseEntity.ok(ApiResponse.ok(cmd, cmd == null ? "No pending command" : "OK")));
    }

    private void completeBatch(
            DeferredResult<ResponseEntity<ApiResponse<Map<String, NextCommandResponse>>>> result,
            Map<String, NextCommandResponse> commands,
            long startedAt,
            List<String> deviceKeys
    ) {
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        log.debug("COMMAND next-batch completed keys={} any={} durationMs={}", deviceKeys, hasAnyCommand(commands), elapsedMs);
        result.setResult(batchResponse(commands));
    }

    private ResponseEntity<ApiResponse<NextCommandResponse>> emptyCommandResponse() {
        return ResponseEntity.ok(ApiResponse.ok(null, "No pending command"));
    }

    private ResponseEntity<ApiResponse<Map<String, NextCommandResponse>>> batchResponse(
            Map<String, NextCommandResponse> commands
    ) {
        return ResponseEntity.ok(ApiResponse.ok(commands, hasAnyCommand(commands) ? "OK" : "No pending command"));
    }

    private boolean hasAnyCommand(Map<String, NextCommandResponse> commands) {
        return commands != null && commands.values().stream().anyMatch(value -> value != null);
    }

    private Map<String, NextCommandResponse> emptyBatch(List<String> keys) {
        LinkedHashMap<String, NextCommandResponse> out = new LinkedHashMap<>();
        keys.forEach(key -> out.put(key, null));
        return out;
    }

    private List<String> parseDeviceKeys(String keys) {
        if (keys == null || keys.isBlank()) {
            throw new BadRequestException("keys required");
        }

        List<String> parsed = java.util.Arrays.stream(keys.split(","))
                .map(String::trim)
                .filter(key -> !key.isBlank())
                .distinct()
                .toList();
        if (parsed.isEmpty() || parsed.size() > 20 || parsed.stream().anyMatch(key -> key.length() > 100)) {
            throw new BadRequestException("Invalid device keys");
        }
        return parsed;
    }
}
