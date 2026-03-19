package com.java.domain.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TelegramNotifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.telegram.enabled:false}")
    private boolean enabled;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.chat-id:}")
    private String chatId;

    public void sendMessage(String text) {
        if (!enabled) {
            log.warn("Telegram disabled");
            return;
        }
        if (botToken == null || botToken.isBlank()) {
            log.warn("Telegram bot token blank");
            return;
        }
        if (chatId == null || chatId.isBlank()) {
            log.warn("Telegram chat id blank");
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> request = new HashMap<>();
        request.put("chat_id", chatId);
        request.put("text", text);

        try {
            Map response = restTemplate.postForObject(url, request, Map.class);
            log.info("Telegram response={}", response);
        } catch (Exception ex) {
            log.error("Failed to send telegram to chatId={}", chatId, ex);
            throw ex;
        }
    }
}