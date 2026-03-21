import { getDeviceType } from "./deviceUtils";
import { mapTelemetryToMonitoring } from "./monitoringUtils";

export function mapRealtimeStateToDevicePatch(device, payload) {
  const type = getDeviceType(device);

  if (type === "FAN") {
    return {
      fanStatus: payload.status ?? device.fanStatus,
      status: payload.status ?? device.status,
      fanSpeed: payload.speed ?? payload.fanSpeed ?? device.fanSpeed,
    };
  }

  if (type === "LIGHT") {
    return {
      lightStatus: payload.status ?? device.lightStatus,
      status: payload.status ?? device.status,
      brightness: payload.brightness ?? payload.level ?? device.brightness,
    };
  }

  return payload;
}

export function applyRealtimeEventToDashboard(prev, event) {
  if (!prev || !event || typeof event !== "object") return prev;

  if (event.type === "DEVICE_STATE_CHANGED") {
    const deviceId = Number(event.deviceId);

    return {
      ...prev,
      devices: (prev.devices || []).map((device) => {
        if (Number(device.id) !== deviceId) return device;

        return {
          ...device,
          ...mapRealtimeStateToDevicePatch(device, event.payload || {}),
        };
      }),
    };
  }

  if (event.type === "HOME_MODE_CHANGED") {
    const controllerId = Number(event.deviceId);

    return {
      ...prev,
      devices: (prev.devices || []).map((device) => {
        if (Number(device.id) !== controllerId) return device;

        return {
          ...device,
          mode: event.payload?.mode || device.mode,
        };
      }),
    };
  }

  return prev;
}

export function applyRealtimeEventToMonitoring(prev, event, activeConfig, devices) {
  if (!Array.isArray(prev)) return prev;
  if (!event || event.type !== "TELEMETRY_RECEIVED") return prev;

  const slots = activeConfig?.monitoringSlots || {};
  const telemetryDeviceId = Number(event.deviceId);
  const device = (devices || []).find((d) => Number(d.id) === telemetryDeviceId);

  if (!device) return prev;

  const latest = {
    value_numeric: event.payload?.valueNumeric,
    value_text: event.payload?.valueText,
    value_boolean: event.payload?.valueBoolean,
    created_at: event.payload?.createdAt,
  };

  const mapped = mapTelemetryToMonitoring(device, latest);
  if (!mapped) return prev;

  const belongsToConfiguredSlot =
    telemetryDeviceId === Number(slots.temperatureDeviceId) ||
    telemetryDeviceId === Number(slots.humidityDeviceId) ||
    telemetryDeviceId === Number(slots.lightSensorDeviceId) ||
    telemetryDeviceId === Number(slots.motionDeviceId);

  if (!belongsToConfiguredSlot) return prev;

  let updated = false;

  const next = prev.map((item) => {
    if (item.id === mapped.id) {
      updated = true;
      return mapped;
    }
    return item;
  });

  return updated ? next : [...next, mapped];
}

export function applyOptimisticDeviceToggle(prev, deviceId, nextValue) {
  if (!prev) return prev;

  const isOn = String(nextValue).toUpperCase() === "ON";

  return {
    ...prev,
    devices: (prev.devices || []).map((device) => {
      if (Number(device.id) !== Number(deviceId)) return device;

      const type = getDeviceType(device);

      if (type === "FAN") {
        return {
          ...device,
          fanStatus: isOn ? "ON" : "OFF",
          status: isOn ? "ON" : "OFF",
          fanSpeed: isOn ? device.fanSpeed ?? 100 : 0,
        };
      }

      if (type === "LIGHT") {
        return {
          ...device,
          lightStatus: isOn ? "ON" : "OFF",
          status: isOn ? "ON" : "OFF",
          brightness: isOn ? device.brightness ?? 100 : 0,
        };
      }

      return device;
    }),
  };
}

export function applyOptimisticDeviceIntensity(prev, deviceId, intensity) {
  if (!prev) return prev;

  return {
    ...prev,
    devices: (prev.devices || []).map((device) => {
      if (Number(device.id) !== Number(deviceId)) return device;

      const type = getDeviceType(device);

      if (type === "FAN") {
        return {
          ...device,
          fanSpeed: intensity,
          fanStatus: intensity > 0 ? "ON" : "OFF",
          status: intensity > 0 ? "ON" : "OFF",
        };
      }

      if (type === "LIGHT") {
        return {
          ...device,
          brightness: intensity,
          lightStatus: intensity > 0 ? "ON" : "OFF",
          status: intensity > 0 ? "ON" : "OFF",
        };
      }

      return device;
    }),
  };
}

export function applyOptimisticModeChange(prev, controllerId, mode) {
  if (!prev) return prev;

  return {
    ...prev,
    devices: (prev.devices || []).map((device) => {
      if (Number(device.id) !== Number(controllerId)) return device;

      return {
        ...device,
        mode: String(mode || "").toUpperCase(),
      };
    }),
  };
}