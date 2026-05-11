export function normalizeDeviceType(rawType) {
  const value = String(rawType || "").trim().toUpperCase();

  if (
    [
      "TEMPERATURE",
      "TEMP",
      "TEMP_SENSOR",
      "TEMPERATURE_SENSOR",
      "TEMPERATURE_NODE",
    ].includes(value)
  ) {
    return "TEMPERATURE_NODE";
  }

  if (
    ["HUMIDITY", "HUMIDITY_SENSOR", "HUMIDITY_NODE", "HUMID"].includes(value)
  ) {
    return "HUMIDITY_NODE";
  }

  if (
    [
      "LIGHT_SENSOR",
      "LDR",
      "LUX",
      "LIGHT_NODE",
      "BRIGHTNESS_SENSOR",
      "ILLUMINANCE",
    ].includes(value)
  ) {
    return "LIGHT_NODE";
  }

  if (
    ["MOTION", "MOTION_SENSOR", "MOTION_NODE", "PIR", "PIR_SENSOR"].includes(
      value
    )
  ) {
    return "MOTION_NODE";
  }

  if (["FAN", "SMART_FAN"].includes(value)) {
    return "FAN";
  }

  if (["LIGHT", "SMART_LIGHT", "LAMP", "BULB"].includes(value)) {
    return "LIGHT";
  }

  if (["SMART_CONTROLLER", "CONTROLLER"].includes(value)) {
    return "SMART_CONTROLLER";
  }

  return value;
}

export function getDeviceType(device) {
  return normalizeDeviceType(
    device?.subtype || device?.type || device?.deviceType || device?.name
  );
}

export function getDeviceClass(device) {
  return String(device?.deviceClass || device?.class || "")
    .trim()
    .toUpperCase();
}

export function isControllableDevice(device) {
  const deviceClass = getDeviceClass(device);
  const type = getDeviceType(device);

  if (deviceClass && deviceClass !== "ACTUATOR") {
    return false;
  }

  return type === "FAN" || type === "LIGHT";
}

export function isControllerDevice(device) {
  const deviceClass = getDeviceClass(device);
  const subtype = getDeviceType(device);

  return deviceClass === "CONTROLLER" || subtype === "SMART_CONTROLLER";
}

export function toDeviceId(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
}

export function findConfiguredDevice(
  devices,
  candidateIds,
  expectedType,
  { requireControllable = false } = {}
) {
  const normalizedIds = candidateIds.map(toDeviceId).filter((id) => id !== null);

  if (normalizedIds.length === 0) {
    return null;
  }

  return (
    devices.find((device) => {
      const deviceId = toDeviceId(device?.id);
      const deviceType = getDeviceType(device);
      return (
        normalizedIds.includes(deviceId) &&
        deviceType === expectedType &&
        (!requireControllable || isControllableDevice(device))
      );
    }) || null
  );
}

export function findFirstControllableDevice(devices, expectedType) {
  return (
    devices.find(
      (device) =>
        getDeviceType(device) === expectedType && isControllableDevice(device)
    ) || null
  );
}

export function buildConfiguredDashboardDevices(devices, slots) {
  const configured = [
    findConfiguredDevice(devices, [slots?.fanDeviceId], "FAN", {
      requireControllable: true,
    }) ||
      findFirstControllableDevice(devices, "FAN"),
    findConfiguredDevice(devices, [slots?.lightDeviceId], "LIGHT", {
      requireControllable: true,
    }) ||
      findFirstControllableDevice(devices, "LIGHT"),
  ].filter(Boolean);

  const uniqueMap = new Map();
  configured.forEach((device) => {
    uniqueMap.set(device.id, device);
  });

  return [...uniqueMap.values()];
}

export function clampPercent(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) return 0;
  if (num < 0) return 0;
  if (num > 100) return 100;
  return Math.round(num);
}

export function normalizePercent(value, status) {
  const num = Number(value);

  if (Number.isFinite(num)) {
    return clampPercent(num);
  }

  return String(status || "").toUpperCase() === "ON" ? 100 : 0;
}

export function resolveEnabled(device) {
  const type = getDeviceType(device);

  if (type === "FAN") {
    return String(device.fanStatus || device.status || "").toUpperCase() === "ON";
  }

  if (type === "LIGHT") {
    return String(device.lightStatus || device.status || "").toUpperCase() === "ON";
  }

  return false;
}

export function resolveIntensity(device) {
  const type = getDeviceType(device);

  if (type === "FAN") {
    const value =
      device.fanSpeed ?? device.speed ?? device.intensity ?? device.level;

    return normalizePercent(value, device.fanStatus || device.status);
  }

  if (type === "LIGHT") {
    const value =
      device.brightness ??
      device.lightLevel ??
      device.intensity ??
      device.level;

    return normalizePercent(value, device.lightStatus || device.status);
  }

  return 0;
}

export function buildDeviceModeText(type, enabled, intensity) {
  if (type === "FAN") {
    return enabled ? `Fan running • ${intensity}%` : "Fan is off";
  }

  if (type === "LIGHT") {
    return enabled ? `Light on • ${intensity}%` : "Light is off";
  }

  return "Ready";
}

export function resolveToggleTarget(device) {
  const targetDevice = device?.raw || device;
  const type = getDeviceType(targetDevice);

  if (type === "FAN") return "fan";
  if (type === "LIGHT") return "light";

  return null;
}

export function resolveIntensityTarget(device) {
  const targetDevice = device?.raw || device;
  const type = getDeviceType(targetDevice);

  if (type === "FAN") return "fanSpeed";
  if (type === "LIGHT") return "brightness";

  return null;
}

export function resolveDeviceIcon(type) {
  if (type === "FAN") return "fan";
  if (type === "LIGHT") return "bulb";
  return "bulb";
}

export function mapDeviceToCardModel(device) {
  const type = getDeviceType(device);
  const intensity = resolveIntensity(device);
  const enabled = resolveEnabled(device);

  return {
    id: device.id,
    name: `${device.name} (${type})`,
    enabled,
    intensity,
    intensityLabel: type === "FAN" ? "Speed" : "Brightness",
    supportsIntensity: type === "FAN" || type === "LIGHT",
    icon: resolveDeviceIcon(type),
    modeText: buildDeviceModeText(type, enabled, intensity),
    raw: device,
  };
}
