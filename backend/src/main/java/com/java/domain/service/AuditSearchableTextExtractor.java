package com.java.domain.service;
import java.util.Locale;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class AuditSearchableTextExtractor {

    private final ObjectMapper objectMapper;

    public String extract(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(' ')
                  .append(extract(entry.getKey()))
                  .append(' ')
                  .append(extract(entry.getValue()));
            }
            return sb.toString().toLowerCase(Locale.ROOT);
        }

        if (value instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object item : iterable) {
                sb.append(' ').append(extract(item));
            }
            return sb.toString().toLowerCase(Locale.ROOT);
        }

        String text = String.valueOf(value).trim();
        if (text.isBlank() || "-".equals(text)) {
            return "";
        }

        try {
            Map<String, Object> map = objectMapper.readValue(
                    text,
                    new TypeReference<Map<String, Object>>() {}
            );
            return extract(map);
        } catch (JacksonException ignored) {
            return text.toLowerCase(Locale.ROOT);
        }
    }
}