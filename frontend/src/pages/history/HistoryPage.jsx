import { useEffect, useMemo, useState } from "react";
import "./HistoryPage.css";
import {
  fetchHistoryDevicesByHomeId,
  fetchHistoryTelemetry,
  fetchHistoryActiveConfigByHomeId,
} from "../../api/historyApi";
import DeviceFilterBar from "../../components/history/DeviceFilterBar";
import TimeRangeTabs from "../../components/history/TimeRangeTabs";
import HistoryChartCard from "../../components/history/HistoryChartCard";
import { useAuth } from "../../providers/AuthProvider";

const TIME_RANGE_OPTIONS = [
  { label: "2 Hours", value: "2h" },
  { label: "6 Hours", value: "6h" },
  { label: "12 Hours", value: "12h" },
  { label: "24 Hours", value: "24h" },
  { label: "7 Days", value: "7d" },
];

function HistoryPage() {
  const [devices, setDevices] = useState([]);
  const [activeConfig, setActiveConfig] = useState(null);
  const [chartItems, setChartItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingCharts, setLoadingCharts] = useState(false);
  const [error, setError] = useState("");
  const [activeTimeRange, setActiveTimeRange] = useState("12h");
  const [selectedFilters, setSelectedFilters] = useState([]);
  const { user: currentUser } = useAuth();
  const homeId = currentUser?.homeId ? Number(currentUser.homeId) : null;

  const configuredDevices = useMemo(() => {
    return buildConfiguredHistoryDevices(
      devices,
      activeConfig?.monitoringSlots || {}
    );
  }, [devices, activeConfig]);

  useEffect(() => {
    if (!homeId) {
      setDevices([]);
      setActiveConfig(null);
      setChartItems([]);
      setLoading(false);
      return;
    }

    loadDevices();
  }, [homeId]);

  useEffect(() => {
    const nextSelected = getDefaultSelectedFilters(configuredDevices);
    setSelectedFilters((prev) => {
      if (
        prev.length > 0 &&
        prev.every((item) => nextSelected.includes(item))
      ) {
        return prev;
      }
      return nextSelected;
    });
  }, [configuredDevices]);

  useEffect(() => {
    if (configuredDevices.length > 0) {
      loadCharts(configuredDevices, activeTimeRange);
    } else {
      setChartItems([]);
    }
  }, [configuredDevices, activeTimeRange]);

  async function loadDevices() {
    try {
      setLoading(true);
      setError("");

      const [deviceList, config] = await Promise.all([
        fetchHistoryDevicesByHomeId(homeId),
        fetchHistoryActiveConfigByHomeId(homeId),
      ]);

      setDevices(Array.isArray(deviceList) ? deviceList : []);
      setActiveConfig(config || null);
    } catch (err) {
      setError(err?.message || "Failed to load history data");
      setDevices([]);
      setActiveConfig(null);
    } finally {
      setLoading(false);
    }
  }

  async function loadCharts(rawDevices, range) {
    try {
      setLoadingCharts(true);
      setError("");

      const supportedDevices = rawDevices.filter(isHistorySupportedDevice);

      const results = await Promise.all(
        supportedDevices.map(async (device) => {
          try {
            const telemetryData = await fetchHistoryTelemetry(device.deviceKey, range);
            return mapDeviceToHistoryChart(device, telemetryData, activeConfig);
          } catch {
            return mapDeviceToHistoryChart(device, null, activeConfig);
          }
        })
      );

      setChartItems(results.filter(Boolean));
    } catch (err) {
      setError(err?.message || "Failed to load history charts");
      setChartItems([]);
    } finally {
      setLoadingCharts(false);
    }
  }

  const filterOptions = useMemo(() => {
    const availableTypes = new Set(configuredDevices.map(getDeviceType));

    const options = [
      { value: "ALL", label: "All", kind: "all" },
      { value: "SENSORS", label: "Sensors only", kind: "group" },
      { value: "ACTUATORS", label: "Actuators only", kind: "group" },
    ];

    const typedOptions = [
      {
        value: "TEMPERATURE_NODE",
        label: "Temperature Sensor",
        tone: "blue",
      },
      {
        value: "HUMIDITY_NODE",
        label: "Humidity Sensor",
        tone: "orange",
      },
      {
        value: "LIGHT_NODE",
        label: "Light Sensor",
        tone: "yellow",
      },
      {
        value: "FAN",
        label: "Fan",
        tone: "green",
      },
      {
        value: "LIGHT",
        label: "Smart Light",
        tone: "purple",
      },
    ].filter((item) => availableTypes.has(item.value));

    return [...options, ...typedOptions];
  }, [configuredDevices]);

  const visibleCharts = useMemo(() => {
    return chartItems.filter((item) => selectedFilters.includes(item.deviceType));
  }, [chartItems, selectedFilters]);

  function handleSelectFilter(filterValue) {
    if (filterValue === "ALL") {
      setSelectedFilters(getAllTypedFilterValues(filterOptions));
      return;
    }

    if (filterValue === "SENSORS") {
      const sensors = ["TEMPERATURE_NODE", "HUMIDITY_NODE", "LIGHT_NODE"].filter(
        (value) => filterOptions.some((item) => item.value === value)
      );
      setSelectedFilters(sensors);
      return;
    }

    if (filterValue === "ACTUATORS") {
      const actuators = ["FAN", "LIGHT"].filter((value) =>
        filterOptions.some((item) => item.value === value)
      );
      setSelectedFilters(actuators);
      return;
    }

    setSelectedFilters((prev) => {
      if (prev.includes(filterValue)) {
        const next = prev.filter((item) => item !== filterValue);
        return next.length === 0 ? prev : next;
      }

      return [...prev, filterValue];
    });
  }

  return (
    <div className="history-page">
      <div className="history-content">
        {loading ? (
          <section className="history-panel">
            <div className="history-panel__body">Loading...</div>
          </section>
        ) : (
          <>
            <section className="history-panel history-analytics-panel">
              <div className="history-panel__header">
                <h2>Environment Data Analytics &amp; History</h2>
              </div>

              <div className="history-panel__body">
                <DeviceFilterBar
                  title="Select Devices"
                  options={filterOptions}
                  selectedValues={selectedFilters}
                  onSelect={handleSelectFilter}
                />

                <TimeRangeTabs
                  title="Time Range"
                  options={TIME_RANGE_OPTIONS}
                  activeValue={activeTimeRange}
                  onChange={setActiveTimeRange}
                />
              </div>
            </section>

            {loadingCharts ? (
              <section className="history-panel">
                <div className="history-panel__body">Loading charts...</div>
              </section>
            ) : (
              <section className="history-chart-grid">
                {visibleCharts.length > 0 ? (
                  visibleCharts.map((item) => (
                    <HistoryChartCard key={item.id} item={item} />
                  ))
                ) : (
                  <section className="history-panel">
                    <div className="history-panel__body">
                      No matching history data available
                    </div>
                  </section>
                )}
              </section>
            )}

            {error ? (
              <div className="history-error-text" role="alert">
                {error}
              </div>
            ) : null}
          </>
        )}
      </div>
    </div>
  );
}

function extractTelemetryItems(payload) {
  if (!payload) return [];

  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload.items)) return payload.items;
  if (Array.isArray(payload.telemetry)) return payload.telemetry;
  if (Array.isArray(payload.data)) return payload.data;
  if (Array.isArray(payload.points)) return payload.points;
  if (Array.isArray(payload.history)) return payload.history;

  return [];
}

function extractTelemetrySeries(payload) {
  if (!payload || typeof payload !== "object") {
    return {};
  }

  const series = payload.series;
  if (!series || typeof series !== "object") {
    return {};
  }

  return series;
}

function getDefaultSelectedFilters(devices) {
  return [...new Set(devices.map(getDeviceType).filter(Boolean))];
}

function getAllTypedFilterValues(filterOptions) {
  return filterOptions
    .filter((item) => !["ALL", "SENSORS", "ACTUATORS"].includes(item.value))
    .map((item) => item.value);
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

  return value;
}

function getDeviceType(device) {
  return normalizeDeviceType(
    device?.subtype || device?.type || device?.deviceType || device?.name
  );
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

function buildConfiguredHistoryDevices(devices, slots) {
  const configured = [
    findConfiguredDevice(devices, [slots?.temperatureDeviceId], "TEMPERATURE_NODE"),
    findConfiguredDevice(devices, [slots?.humidityDeviceId], "HUMIDITY_NODE"),
    findConfiguredDevice(devices, [slots?.lightSensorDeviceId], "LIGHT_NODE"),
    findConfiguredDevice(devices, [slots?.fanDeviceId], "FAN"),
    findConfiguredDevice(devices, [slots?.lightDeviceId], "LIGHT"),
  ].filter(Boolean);

  const uniqueMap = new Map();
  configured.forEach((device) => {
    uniqueMap.set(device.id, device);
  });

  return [...uniqueMap.values()];
}

function getDeviceClass(device) {
  return String(device?.deviceClass || device?.class || "").toUpperCase();
}

function isHistorySupportedDevice(device) {
  const type = getDeviceType(device);
  return [
    "TEMPERATURE_NODE",
    "HUMIDITY_NODE",
    "LIGHT_NODE",
    "FAN",
    "LIGHT",
  ].includes(type);
}

function mapDeviceToHistoryChart(device, telemetryPayload, activeConfig) {
  const type = getDeviceType(device);
  const telemetryItems = extractTelemetryItems(telemetryPayload);
  const telemetrySeries = extractTelemetrySeries(telemetryPayload);

  if (type === "TEMPERATURE_NODE") {
    const points = telemetryItems.map(mapTelemetryPoint).filter(Boolean);

    return {
      id: `history-${device.id}`,
      deviceId: device.id,
      deviceType: type,
      name: device.name || "Temperature Sensor",
      badge: "Sensor",
      badgeTone: "sensor",
      colorTone: "blue",
      icon: "temperature",
      unit: "°C",
      lineType: "line",
      points,
      thresholds: buildTemperatureThresholds(activeConfig),
      stats: buildStats(points, "°C", 1),
    };
  }

  if (type === "HUMIDITY_NODE") {
    const points = telemetryItems.map(mapTelemetryPoint).filter(Boolean);

    return {
      id: `history-${device.id}`,
      deviceId: device.id,
      deviceType: type,
      name: device.name || "Humidity Sensor",
      badge: "Sensor",
      badgeTone: "sensor",
      colorTone: "orange",
      icon: "humidity",
      unit: "%",
      lineType: "line",
      points,
      thresholds: [],
      stats: buildStats(points, "%", 1),
    };
  }

  if (type === "LIGHT_NODE") {
    const points = telemetryItems.map(mapTelemetryPoint).filter(Boolean);

    return {
      id: `history-${device.id}`,
      deviceId: device.id,
      deviceType: type,
      name: device.name || "Light Sensor",
      badge: "Sensor",
      badgeTone: "sensor",
      colorTone: "yellow",
      icon: "light",
      unit: "%",
      lineType: "line",
      points,
      thresholds: buildLightThresholds(activeConfig),
      stats: buildStats(points, "%", 0),
    };
  }

  if (type === "FAN") {
    const powerPoints = (telemetrySeries.power || telemetryItems)
      .map(mapTelemetryPoint)
      .filter(Boolean)
      .map((point) => ({
        ...point,
        value: point.value > 0 ? 1 : 0,
      }));

    const speedPoints = (telemetrySeries.speed || [])
      .map(mapTelemetryPoint)
      .filter(Boolean)
      .map((point) => ({
        ...point,
        value: clamp(point.value, 0, 100),
      }));

    return {
      id: `history-${device.id}`,
      deviceId: device.id,
      deviceType: type,
      name: device.name || "Fan",
      badge: "Actuator",
      badgeTone: "actuator",
      colorTone: "green",
      icon: "fan",
      unit: "%",
      lineType: "step",
      points: speedPoints.length ? speedPoints : powerPoints,
      powerPoints,
      secondaryPoints: speedPoints,
      secondaryLabel: "Speed",
      secondaryUnit: "%",
      thresholds: [],
      stats: buildActuatorStats(powerPoints, speedPoints, "speed"),
      yLabels: speedPoints.length ? undefined : ["OFF", "ON"],
    };
  }

  if (type === "LIGHT") {
    const powerPoints = (telemetrySeries.power || telemetryItems)
      .map(mapTelemetryPoint)
      .filter(Boolean)
      .map((point) => ({
        ...point,
        value: point.value > 0 ? 1 : 0,
      }));

    const brightnessPoints = (telemetrySeries.brightness || [])
      .map(mapTelemetryPoint)
      .filter(Boolean)
      .map((point) => ({
        ...point,
        value: clamp(point.value, 0, 100),
      }));

    return {
      id: `history-${device.id}`,
      deviceId: device.id,
      deviceType: type,
      name: device.name || "Smart Light",
      badge: "Actuator",
      badgeTone: "actuator",
      colorTone: "purple",
      icon: "bulb",
      unit: "%",
      lineType: "step",
      points: brightnessPoints.length ? brightnessPoints : powerPoints,
      powerPoints,
      secondaryPoints: brightnessPoints,
      secondaryLabel: "Brightness",
      secondaryUnit: "%",
      thresholds: [],
      stats: buildActuatorStats(powerPoints, brightnessPoints, "brightness"),
      yLabels: brightnessPoints.length ? undefined : ["OFF", "ON"],
    };
  }

  return null;
}

function mapTelemetryPoint(item) {
  const createdAt =
    item?.created_at ||
    item?.createdAt ||
    item?.timestamp ||
    item?.recordedAt ||
    item?.time;

  const numericValue =
    item?.value_numeric ??
    item?.valueNumber ??
    item?.numericValue ??
    item?.value_number ??
    item?.value;

  const booleanValue =
    item?.value_boolean ??
    item?.valueBoolean ??
    item?.booleanValue;

  const textValue =
    item?.value_text ??
    item?.valueText ??
    item?.textValue ??
    item?.state;

  const timestamp = new Date(createdAt).getTime();
  if (Number.isNaN(timestamp)) return null;

  let value = null;

  if (numericValue !== null && numericValue !== undefined && numericValue !== "") {
    const parsed = Number(numericValue);
    if (!Number.isNaN(parsed)) value = parsed;
  } else if (typeof booleanValue === "boolean") {
    value = booleanValue ? 1 : 0;
  } else {
    const normalized = String(textValue || "")
      .trim()
      .toLowerCase();

    if (["on", "true", "1", "open", "running"].includes(normalized)) {
      value = 1;
    } else if (
      ["off", "false", "0", "close", "closed", "idle"].includes(normalized)
    ) {
      value = 0;
    }
  }

  if (value === null || Number.isNaN(value)) return null;

  return {
    time: timestamp,
    label: formatTimeLabel(createdAt),
    value,
  };
}

function buildTemperatureThresholds(activeConfig) {
  const t = activeConfig?.thresholds || {};

  return [
    isFiniteNumber(t.tCritical) && { label: "T_critical", value: t.tCritical, tone: "red" },
    isFiniteNumber(t.tHigh) && { label: "T_high", value: t.tHigh, tone: "orange" },
    isFiniteNumber(t.tLow) && { label: "T_low", value: t.tLow, tone: "blue" },
  ].filter(Boolean);
}

function buildLightThresholds(activeConfig) {
  const t = activeConfig?.thresholds || {};

  return [
    isFiniteNumber(t.lHigh) && { label: "L_high", value: t.lHigh, tone: "orange" },
    isFiniteNumber(t.lLow) && { label: "L_low", value: t.lLow, tone: "blue" },
  ].filter(Boolean);
}

function buildStats(points, unit, fractionDigits = 0) {
  if (!points.length) {
    return [
      { label: "Min", value: `--${unit}` },
      { label: "Avg", value: `--${unit}` },
      { label: "Max", value: `--${unit}` },
    ];
  }

  const values = points.map((item) => item.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const avg = values.reduce((sum, item) => sum + item, 0) / values.length;

  return [
    { label: "Min", value: `${min.toFixed(fractionDigits)}${unit}` },
    { label: "Avg", value: `${avg.toFixed(fractionDigits)}${unit}` },
    { label: "Max", value: `${max.toFixed(fractionDigits)}${unit}` },
  ];
}

function buildBinaryStats(points) {
  if (!points.length) {
    return [
      { label: "State Min", value: "--" },
      { label: "On Ratio", value: "--" },
      { label: "State Max", value: "--" },
    ];
  }

  const values = points.map((item) => item.value);
  const totalOn = values.filter((item) => item > 0).length;
  const avg = (totalOn / values.length) * 100;

  return [
    { label: "State Min", value: Math.min(...values) > 0 ? "ON" : "OFF" },
    { label: "On Ratio", value: `${avg.toFixed(0)}%` },
    { label: "State Max", value: Math.max(...values) > 0 ? "ON" : "OFF" },
  ];
}

function buildActuatorStats(powerPoints, valuePoints, mode) {
  if (valuePoints?.length) {
    const unit = "%";
    const values = valuePoints.map((item) => item.value);
    const avg = values.reduce((sum, item) => sum + item, 0) / values.length;

    return [
      {
        label: mode === "speed" ? "Min Speed" : "Min Brightness",
        value: `${Math.min(...values).toFixed(0)}${unit}`,
      },
      {
        label: mode === "speed" ? "Avg Speed" : "Avg Brightness",
        value: `${avg.toFixed(0)}${unit}`,
      },
      {
        label: mode === "speed" ? "Max Speed" : "Max Brightness",
        value: `${Math.max(...values).toFixed(0)}${unit}`,
      },
    ];
  }

  return buildBinaryStats(powerPoints || []);
}

function isFiniteNumber(value) {
  return Number.isFinite(Number(value));
}

function clamp(value, min, max) {
  const num = Number(value);
  if (!Number.isFinite(num)) return min;
  return Math.min(max, Math.max(min, num));
}

function formatTimeLabel(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--:--";

  return date.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

export default HistoryPage;