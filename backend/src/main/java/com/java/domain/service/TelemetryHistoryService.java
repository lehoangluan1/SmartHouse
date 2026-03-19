package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.config.NotFoundException;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.DeviceRepository;
import com.java.persistence.repo.DeviceStateHistoryRepository;
import com.java.persistence.repo.SensorDataRepository;
import com.java.persistence.repo.projection.DeviceStateHistoryView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelemetryHistoryService {

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final DeviceStateHistoryRepository deviceStateHistoryRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getHistory(String deviceKey, String range) {
        DeviceEntity device = deviceRepository.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new NotFoundException("Device not found with key: " + deviceKey));

        OffsetDateTime to = OffsetDateTime.now();
        OffsetDateTime from = resolveFrom(to, range);

        String subtype = normalize(device.getSubtype());
        String deviceClass = normalize(device.getDeviceClass() != null ? device.getDeviceClass().name() : null);

        List<Map<String, Object>> items;
        Map<String, Object> series = new LinkedHashMap<>();

        if (isActuatorHistoryDevice(subtype, deviceClass)) {
            List<DeviceStateHistoryView> history = deviceStateHistoryRepository.findRange(device.getId(), from, to);

            items = history.stream()
                    .map(this::mapActuatorRow)
                    .toList();

            series = buildActuatorSeries(history);
        } else {
            items = sensorDataRepository.findRange(device.getId(), from, to)
                    .stream()
                    .map(t -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", t.getId());
                        row.put("sensor_id", t.getSensor() != null ? t.getSensor().getId() : null);
                        row.put("sensor_name", t.getSensor() != null ? t.getSensor().getName() : null);
                        row.put("sensor_kind", t.getSensor() != null ? t.getSensor().getSensorKind() : null);
                        row.put("value_numeric", t.getValueNumeric());
                        row.put("value_text", t.getValueText());
                        row.put("value_boolean", t.getValueBoolean());
                        row.put("created_at", t.getCreatedAt());
                        return row;
                    })
                    .toList();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("device_key", device.getDeviceKey());
        body.put("device_id", device.getId());
        body.put("device_name", device.getName());
        body.put("device_class", deviceClass);
        body.put("device_type", subtype);
        body.put("range", range);
        body.put("from", from);
        body.put("to", to);
        body.put("items", items);
        body.put("series", series);

        return body;
    }

    private Map<String, Object> mapActuatorRow(DeviceStateHistoryView t) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", t.getId());
        row.put("capability_code", t.getCapabilityCode());
        row.put("value_numeric", t.getValueNumber());
        row.put("value_text", t.getValueText());
        row.put("value_boolean", t.getValueBoolean());
        row.put("created_at", t.getCreatedAt());
        return row;
    }

    private Map<String, Object> buildActuatorSeries(List<DeviceStateHistoryView> history) {
        Map<String, List<Map<String, Object>>> grouped = history.stream()
                .collect(Collectors.groupingBy(
                        item -> normalize(item.getCapabilityCode()),
                        LinkedHashMap::new,
                        Collectors.mapping(this::mapActuatorRow, Collectors.toList())
                ));

        Map<String, Object> series = new LinkedHashMap<>();
        grouped.forEach((key, value) -> series.put(key.toLowerCase(), value));
        return series;
    }

    private boolean isActuatorHistoryDevice(String subtype, String deviceClass) {
        return "ACTUATOR".equals(deviceClass) || "FAN".equals(subtype) || "LIGHT".equals(subtype);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private OffsetDateTime resolveFrom(OffsetDateTime to, String range) {
        if (range == null || range.isBlank()) {
            return to.minusHours(24);
        }

        String normalized = range.trim().toLowerCase();

        try {
            if (normalized.endsWith("h")) {
                long hours = Long.parseLong(normalized.substring(0, normalized.length() - 1));
                if (hours <= 0) {
                    throw new BadRequestException("range hours must be greater than 0");
                }
                return to.minusHours(hours);
            }

            if (normalized.endsWith("d")) {
                long days = Long.parseLong(normalized.substring(0, normalized.length() - 1));
                if (days <= 0) {
                    throw new BadRequestException("range days must be greater than 0");
                }
                return to.minusDays(days);
            }
        } catch (NumberFormatException ex) {
            throw new BadRequestException("invalid range. Valid examples: 1h, 2h, 6h, 12h, 24h, 7d");
        }

        throw new BadRequestException("invalid range. Valid examples: 1h, 2h, 6h, 12h, 24h, 7d");
    }
}