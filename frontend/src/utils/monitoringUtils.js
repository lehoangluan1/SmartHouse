import { fetchDeviceTelemetry } from "../api/dashboardApi";
import { findConfiguredDevice, getDeviceType } from "./deviceUtils";

export function createMonitoringFallbackItems() {
  return {
    temperature: {
      id: "temperature",
      label: "Temperature",
      value: "--°C",
      updatedText: "No data",
      icon: "temperature",
    },
    humidity: {
      id: "humidity",
      label: "Humidity",
      value: "--%",
      updatedText: "No data",
      icon: "humidity",
    },
    light: {
      id: "light",
      label: "Light Intensity",
      value: "--%",
      updatedText: "No data",
      icon: "light",
    },
    motion: {
      id: "motion",
      label: "Object Detector",
      value: "No object",
      updatedText: "No data",
      icon: "motion",
      status: "inactive",
    },
  };
}

export function resolveMotionDetected({ numericValue, textValue, booleanValue }) {
  if (typeof booleanValue === "boolean") return booleanValue;

  if (numericValue !== null && numericValue !== undefined) {
    return Number(numericValue) > 0;
  }

  const normalizedText = String(textValue || "").trim().toLowerCase();

  return (
    normalizedText === "1" ||
    normalizedText === "true" ||
    normalizedText === "on" ||
    normalizedText === "motion" ||
    normalizedText === "detected" ||
    normalizedText === "object" ||
    normalizedText === "yes"
  );
}

export function formatUpdatedText(createdAt) {
  const time = new Date(createdAt).getTime();
  if (Number.isNaN(time)) return "Updated recently";

  const diffSeconds = Math.max(0, Math.floor((Date.now() - time) / 1000));

  if (diffSeconds < 60) return `Updated ${diffSeconds}s ago`;

  const diffMinutes = Math.floor(diffSeconds / 60);
  if (diffMinutes < 60) return `Updated ${diffMinutes}m ago`;

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `Updated ${diffHours}h ago`;

  const diffDays = Math.floor(diffHours / 24);
  return `Updated ${diffDays}d ago`;
}

export function mapTelemetryToMonitoring(device, latest) {
  const type = getDeviceType(device);
  const numericValue = latest?.value_numeric;
  const textValue = latest?.value_text;
  const booleanValue = latest?.value_boolean;
  const createdAt = latest?.created_at;

  if (type === "TEMPERATURE_NODE") {
    return {
      id: "temperature",
      label: "Temperature",
      value:
        numericValue !== null && numericValue !== undefined
          ? `${Number(numericValue).toFixed(1)}°C`
          : "--°C",
      updatedText: createdAt ? formatUpdatedText(createdAt) : "No data",
      icon: "temperature",
    };
  }

  if (type === "HUMIDITY_NODE") {
    return {
      id: "humidity",
      label: "Humidity",
      value:
        numericValue !== null && numericValue !== undefined
          ? `${Number(numericValue).toFixed(0)}%`
          : "--%",
      updatedText: createdAt ? formatUpdatedText(createdAt) : "No data",
      icon: "humidity",
    };
  }

  if (type === "LIGHT_NODE") {
    return {
      id: "light",
      label: "Light Intensity",
      value:
        numericValue !== null && numericValue !== undefined
          ? `${Number(numericValue).toFixed(0)}%`
          : "--%",
      updatedText: createdAt ? formatUpdatedText(createdAt) : "No data",
      icon: "light",
    };
  }

  if (type === "MOTION_NODE") {
    const motionDetected = resolveMotionDetected({
      numericValue,
      textValue,
      booleanValue,
    });

    return {
      id: "motion",
      label: "Object Detector",
      value: motionDetected ? "Object detected" : "No object",
      updatedText: createdAt ? formatUpdatedText(createdAt) : "No data",
      icon: "motion",
      status: motionDetected ? "active" : "inactive",
    };
  }

  return null;
}

export async function buildMonitoringFromConfig(devices, activeConfig) {
  const slots = activeConfig?.monitoringSlots;
  const fallbackItems = createMonitoringFallbackItems();

  if (!slots || typeof slots !== "object") {
    return Object.values(fallbackItems);
  }

  const slotDefinitions = [
    {
      slotKeys: ["temperatureDeviceId"],
      expectedType: "TEMPERATURE_NODE",
      fallback: fallbackItems.temperature,
    },
    {
      slotKeys: ["humidityDeviceId"],
      expectedType: "HUMIDITY_NODE",
      fallback: fallbackItems.humidity,
    },
    {
      slotKeys: ["lightSensorDeviceId"],
      expectedType: "LIGHT_NODE",
      fallback: fallbackItems.light,
    },
    {
      slotKeys: ["motionDeviceId"],
      expectedType: "MOTION_NODE",
      fallback: fallbackItems.motion,
    },
  ];

  const monitoringItems = await Promise.all(
    slotDefinitions.map(async ({ slotKeys, expectedType, fallback }) => {
      const device = findConfiguredDevice(
        devices,
        slotKeys.map((key) => slots?.[key]),
        expectedType
      );

      if (!device?.deviceKey) {
        return fallback;
      }

      try {
        const telemetry = await fetchDeviceTelemetry(device.deviceKey, "1h");
        const items = telemetry?.items || [];
        const latest = items[items.length - 1];
        return mapTelemetryToMonitoring(device, latest) || fallback;
      } catch {
        return fallback;
      }
    })
  );

  return monitoringItems;
}