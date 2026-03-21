import { useEffect, useMemo, useState } from "react";
import "./DashboardPage.css";
import {
  controlDevice,
  fetchActiveConfigByHomeId,
  fetchDashboardByHomeId,
  fetchDeviceTelemetry,
} from "../../api/dashboardApi";
import MonitoringCard from "../../components/dashboard/MonitoringCard";
import DeviceSwitchCard from "../../components/dashboard/DeviceSwitchCard";
import SegmentControl from "../../components/dashboard/SegmentControl";
import { useAuth } from "../../providers/AuthProvider";

const SEGMENTS = ["AUTO", "MANUAL", "SLEEP", "AWAY"];

function DashboardPage() {
  const [dashboardData, setDashboardData] = useState(null);
  const [activeConfig, setActiveConfig] = useState(null);
  const [monitoring, setMonitoring] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [selectedDeviceId, setSelectedDeviceId] = useState(null);
  const [intensityDraftMap, setIntensityDraftMap] = useState({});
  const { user: currentUser } = useAuth();
  const homeId = currentUser?.homeId ? Number(currentUser.homeId) : null;

  const rawDevices = Array.isArray(dashboardData?.devices)
    ? dashboardData.devices
    : [];

  const monitoringSlots = activeConfig?.monitoringSlots || {};

  const devices = useMemo(() => {
    return buildConfiguredDashboardDevices(rawDevices, monitoringSlots).map(
      mapDeviceToCardModel
    );
  }, [rawDevices, monitoringSlots]);

  const selectedDevice =
    devices.find((device) => device.id === selectedDeviceId) || null;

  const controllerDevice = useMemo(() => {
    return rawDevices.find(isControllerDevice) || null;
  }, [rawDevices]);

  const activeSegment = controllerDevice?.mode
    ? String(controllerDevice.mode).toUpperCase()
    : "AUTO";

  useEffect(() => {
    if (!homeId) {
      setDashboardData(null);
      setActiveConfig(null);
      setMonitoring([]);
      setSelectedDeviceId(null);
      setLoading(false);
      return;
    }

    loadDashboard();
  }, [homeId]);

  useEffect(() => {
    const nextMap = {};
    devices.forEach((item) => {
      nextMap[item.id] = item.intensity ?? 0;
    });

    setIntensityDraftMap((prev) => {
      const prevKeys = Object.keys(prev);
      const nextKeys = Object.keys(nextMap);

      if (
        prevKeys.length === nextKeys.length &&
        prevKeys.every((key) => prev[key] === nextMap[key])
      ) {
        return prev;
      }

      return nextMap;
    });
  }, [devices]);

  async function loadDashboard() {
    try {
      setLoading(true);
      setError("");

      const [data, fetchedActiveConfig] = await Promise.all([
        fetchDashboardByHomeId(homeId),
        fetchActiveConfigByHomeId(homeId),
      ]);

      setDashboardData(data || null);
      setActiveConfig(fetchedActiveConfig || null);

      const configuredControllableDevices = buildConfiguredDashboardDevices(
        data?.devices || [],
        fetchedActiveConfig?.monitoringSlots || {}
      );

      if (configuredControllableDevices.length > 0) {
        setSelectedDeviceId((prev) => {
          if (prev && configuredControllableDevices.some((d) => d.id === prev)) {
            return prev;
          }
          return configuredControllableDevices[0].id;
        });
      } else {
        setSelectedDeviceId(null);
      }

      const monitoringItems = await buildMonitoringFromConfig(
        data?.devices || [],
        fetchedActiveConfig
      );
      setMonitoring(monitoringItems);
    } catch (err) {
      setError(err?.message || "Failed to load dashboard");
      setDashboardData(null);
      setActiveConfig(null);
      setMonitoring([]);
      setSelectedDeviceId(null);
    } finally {
      setLoading(false);
    }
  }

  async function handleToggleDevice(device) {
    const target = resolveToggleTarget(device);
    const nextValue = device.enabled ? "OFF" : "ON";

    if (!target) return;

    try {
      setActionLoading(true);
      setError("");

      await controlDevice(device.id, {
        target,
        value: nextValue,
        actorId: currentUser?.userId,
        actorName: currentUser?.username ?? "web-user",
        method: "app",
      });

      await loadDashboard();
    } catch (err) {
      setError(err?.message || "Failed to control device");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleIntensityChange(device, intensity) {
    const target = resolveIntensityTarget(device);
    if (!target) return;

    const safeValue = clampPercent(intensity);

    setIntensityDraftMap((prev) => ({
      ...prev,
      [device.id]: safeValue,
    }));

    try {
      setActionLoading(true);
      setError("");

      await controlDevice(device.id, {
        target,
        value: String(safeValue),
        actorId: currentUser?.userId,
        actorName: currentUser?.username ?? "web-user",
        method: "app",
      });

      await loadDashboard();
    } catch (err) {
      setError(err?.message || "Failed to adjust device intensity");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleChangeMode(mode) {
    if (!controllerDevice) return;

    try {
      setActionLoading(true);
      setError("");

      await controlDevice(controllerDevice.id, {
        target: "mode",
        value: mode.toLowerCase(),
        actorId: currentUser?.userId,
        actorName: currentUser?.username ?? "web-user",
        method: "app",
      });

      await loadDashboard();
    } catch (err) {
      setError(err?.message || "Failed to change mode");
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div className="dashboard-page">
      <div className="dashboard-content">
        {loading ? (
          <div className="dashboard-panel">
            <div className="dashboard-panel__body">Loading...</div>
          </div>
        ) : (
          <>
            <section className="dashboard-panel">
              <div className="dashboard-panel__header">
                <h2>Monitoring</h2>
              </div>

              <div className="dashboard-panel__body dashboard-monitoring-list">
                {monitoring.map((item) => (
                  <MonitoringCard key={item.id} item={item} />
                ))}
              </div>
            </section>

            <section className="dashboard-panel">
              <div className="dashboard-panel__header">
                <h2>Control/Switch</h2>
              </div>

              <div className="dashboard-panel__body dashboard-device-list">
                {devices.length > 0 ? (
                  devices.map((device) => (
                    <DeviceSwitchCard
                      key={device.id}
                      device={{
                        ...device,
                        intensity:
                          intensityDraftMap[device.id] ?? device.intensity ?? 0,
                      }}
                      selected={selectedDeviceId === device.id}
                      disabled={actionLoading}
                      onSelect={() => setSelectedDeviceId(device.id)}
                      onToggle={() => handleToggleDevice(device)}
                      onIntensityChange={(value) =>
                        handleIntensityChange(device, value)
                      }
                    />
                  ))
                ) : (
                  <div className="dashboard-panel__body">
                    No active configured controllable devices
                  </div>
                )}
              </div>

              {controllerDevice ? (
                <SegmentControl
                  title={`System Mode${
                    controllerDevice?.name ? ` (${controllerDevice.name})` : ""
                  }`}
                  options={SEGMENTS}
                  activeValue={activeSegment}
                  disabled={actionLoading}
                  onChange={handleChangeMode}
                />
              ) : (
                <div className="dashboard-mode-disabled">
                  No controller found that supports mode
                </div>
              )}

              <div className="dashboard-integrations">
                <div className="dashboard-integration-item">
                  <span className="dashboard-status-dot online" />
                  <span>OhStem</span>
                </div>
                <div className="dashboard-integration-item">
                  <span className="dashboard-status-dot online" />
                  <span>Device Control</span>
                </div>
              </div>

              {selectedDevice ? (
                <div className="dashboard-selected-device-note">
                  Selected: <strong>{selectedDevice.name}</strong>
                </div>
              ) : null}

              {error ? (
                <div className="dashboard-error-text" role="alert">
                  {error}
                </div>
              ) : null}
            </section>
          </>
        )}
      </div>
    </div>
  );
}

function normalizeDeviceType(rawType) {
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
    [
      "HUMIDITY",
      "HUMIDITY_SENSOR",
      "HUMIDITY_NODE",
      "HUMID",
    ].includes(value)
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

function getDeviceType(device) {
  return normalizeDeviceType(
    device?.subtype || device?.type || device?.deviceType || device?.name
  );
}

function getDeviceClass(device) {
  return String(device?.deviceClass || device?.class || "")
    .trim()
    .toUpperCase();
}

function isControllerDevice(device) {
  const deviceClass = getDeviceClass(device);
  const subtype = getDeviceType(device);

  return deviceClass === "CONTROLLER" || subtype === "SMART_CONTROLLER";
}

function toDeviceId(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
}

function findConfiguredDevice(devices, candidateIds, expectedType) {
  const normalizedIds = candidateIds.map(toDeviceId).filter((id) => id !== null);

  if (normalizedIds.length === 0) {
    return null;
  }

  return (
    devices.find((device) => {
      const deviceId = toDeviceId(device?.id);
      const deviceType = getDeviceType(device);
      return normalizedIds.includes(deviceId) && deviceType === expectedType;
    }) || null
  );
}

function buildConfiguredDashboardDevices(devices, slots) {
  const configured = [
    findConfiguredDevice(devices, [slots?.fanDeviceId], "FAN"),
    findConfiguredDevice(devices, [slots?.lightDeviceId], "LIGHT"),
  ].filter(Boolean);  
  console.log(slots?.lightDeviceId);
  const uniqueMap = new Map();
  configured.forEach((device) => {
    uniqueMap.set(device.id, device);
  });

  return [...uniqueMap.values()];
}

async function buildMonitoringFromConfig(devices, activeConfig) {
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

function createMonitoringFallbackItems() {
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

function mapTelemetryToMonitoring(device, latest) {
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

function resolveMotionDetected({ numericValue, textValue, booleanValue }) {
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

function formatUpdatedText(createdAt) {
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

function mapDeviceToCardModel(device) {
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

function resolveEnabled(device) {
  const type = getDeviceType(device);

  if (type === "FAN") {
    return String(device.fanStatus || device.status || "").toUpperCase() === "ON";
  }

  if (type === "LIGHT") {
    return String(device.lightStatus || device.status || "").toUpperCase() === "ON";
  }

  return false;
}

function resolveIntensity(device) {
  const type = getDeviceType(device);

  if (type === "FAN") {
    const value =
      device.fanSpeed ??
      device.speed ??
      device.intensity ??
      device.level;

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

function normalizePercent(value, status) {
  const num = Number(value);

  if (Number.isFinite(num)) {
    return clampPercent(num);
  }

  return String(status || "").toUpperCase() === "ON" ? 100 : 0;
}

function clampPercent(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) return 0;
  if (num < 0) return 0;
  if (num > 100) return 100;
  return Math.round(num);
}

function buildDeviceModeText(type, enabled, intensity) {
  if (type === "FAN") {
    return enabled ? `Fan running • ${intensity}%` : "Fan is off";
  }

  if (type === "LIGHT") {
    return enabled ? `Light on • ${intensity}%` : "Light is off";
  }

  return "Ready";
}

function resolveToggleTarget(device) {
  const targetDevice = device?.raw || device;
  const type = getDeviceType(targetDevice);

  if (type === "FAN") return "fan";
  if (type === "LIGHT") return "light";

  return null;
}

function resolveIntensityTarget(device) {
  const targetDevice = device?.raw || device;
  const type = getDeviceType(targetDevice);

  if (type === "FAN") return "fanSpeed";
  if (type === "LIGHT") return "brightness";

  return null;
}

function resolveDeviceIcon(type) {
  if (type === "FAN") return "fan";
  if (type === "LIGHT") return "bulb";
  return "bulb";
}

export default DashboardPage;