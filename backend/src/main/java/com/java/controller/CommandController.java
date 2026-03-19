package com.java.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.java.config.ApiResponse;
import com.java.config.BadRequestException;
import com.java.controller.dto.CommandAckRequest;
import com.java.controller.dto.NextCommandResponse;
import com.java.domain.service.ControlCommandService;

@RestController
@RequestMapping("/api/v1/device")
public class CommandController {

    private final ControlCommandService controlCommandService;

    public CommandController(ControlCommandService controlCommandService) {
        this.controlCommandService = controlCommandService;
    }

    @GetMapping("/{deviceKey}/commands/next")
    public ResponseEntity<ApiResponse<NextCommandResponse>> next(@PathVariable String deviceKey) {
        NextCommandResponse cmd = controlCommandService.getNextCommand(deviceKey);
        return ResponseEntity.ok(ApiResponse.ok(cmd, cmd == null ? "No pending command" : "OK"));
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
}