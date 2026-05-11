import { useMemo } from "react";

function getDeviceType(device) {
  return String(
    device?.subtype ||
      device?.type ||
      device?.deviceType ||
      ""
  ).toUpperCase();
}

function getDeviceClass(device) {
  const explicitClass = String(device?.deviceClass || device?.class || "").toUpperCase();
  if (explicitClass) return explicitClass;

  const type = getDeviceType(device);
  if (["TEMPERATURE_NODE", "HUMIDITY_NODE", "LIGHT_NODE", "MOTION_NODE"].includes(type)) {
    return "SENSOR_NODE";
  }
  if (["FAN", "LIGHT", "AIR_CONDITIONER"].includes(type)) {
    return "ACTUATOR";
  }
  return "";
}

function matchesDevice(device, expectedClass, expectedType) {
  return (
    getDeviceClass(device) === expectedClass &&
    getDeviceType(device) === expectedType
  );
}

function getDeviceLabel(device) {
  const name = device?.name || `Device #${device?.id ?? ""}`;
  const key = device?.deviceKey ? ` (${device.deviceKey})` : "";
  const meta = [getDeviceClass(device), getDeviceType(device)].filter(Boolean).join("/");
  return meta ? `${name}${key} - ${meta}` : `${name}${key}`;
}

function findDevice(devices, deviceId) {
  return (devices || []).find((device) => Number(device.id) === Number(deviceId));
}

function MonitoringDeviceSelector({
  devices = [],
  value,
  disabled = false,
  isAdmin = false,
  onChange,
  onCreateDevice,
}) {
  const temperatureDevices = useMemo(
    () => devices.filter((device) => matchesDevice(device, "SENSOR_NODE", "TEMPERATURE_NODE")),
    [devices]
  );

  const humidityDevices = useMemo(
    () => devices.filter((device) => matchesDevice(device, "SENSOR_NODE", "HUMIDITY_NODE")),
    [devices]
  );

  const lightSensorDevices = useMemo(
    () => devices.filter((device) => matchesDevice(device, "SENSOR_NODE", "LIGHT_NODE")),
    [devices]
  );

  const motionDevices = useMemo(
    () => devices.filter((device) => matchesDevice(device, "SENSOR_NODE", "MOTION_NODE")),
    [devices]
  );

  const fanDevices = useMemo(
    () => devices.filter((device) => matchesDevice(device, "ACTUATOR", "FAN")),
    [devices]
  );

  const lightDevices = useMemo(
    () => devices.filter((device) => matchesDevice(device, "ACTUATOR", "LIGHT")),
    [devices]
  );

  return (
    <div className="config-device-selector-list">
      <div className="config-device-selector-list__header">
        <div>
          <h3>Monitoring devices</h3>
          <p>Select sensors and actuators used in monitoring and automation.</p>
        </div>
      </div>

      <SelectorRow
        iconType="temperature"
        label="Temperature Sensor"
        subtitle="Sensor used for Temperature card in Monitoring"
        options={temperatureDevices}
        devices={devices}
        value={value?.temperatureDeviceId}
        expectedClass="SENSOR_NODE"
        expectedType="TEMPERATURE_NODE"
        disabled={disabled}
        onChange={(deviceId) =>
          onChange?.("temperatureDeviceId", deviceId ? Number(deviceId) : null)
        }
      />

      <SelectorRow
        iconType="humidity"
        label="Humidity Sensor"
        subtitle="Sensor used for Humidity card in Monitoring"
        options={humidityDevices}
        devices={devices}
        value={value?.humidityDeviceId}
        expectedClass="SENSOR_NODE"
        expectedType="HUMIDITY_NODE"
        disabled={disabled}
        onChange={(deviceId) =>
          onChange?.("humidityDeviceId", deviceId ? Number(deviceId) : null)
        }
      />

      <SelectorRow
        iconType="light-sensor"
        label="Light Sensor"
        subtitle="Sensor used for ambient light monitoring"
        options={lightSensorDevices}
        devices={devices}
        value={value?.lightSensorDeviceId}
        expectedClass="SENSOR_NODE"
        expectedType="LIGHT_NODE"
        disabled={disabled}
        onChange={(deviceId) =>
          onChange?.("lightSensorDeviceId", deviceId ? Number(deviceId) : null)
        }
      />

      <SelectorRow
        iconType="motion"
        label="Motion Sensor"
        subtitle="Sensor used for Motion card in Monitoring"
        options={motionDevices}
        devices={devices}
        value={value?.motionDeviceId}
        expectedClass="SENSOR_NODE"
        expectedType="MOTION_NODE"
        disabled={disabled}
        onChange={(deviceId) =>
          onChange?.("motionDeviceId", deviceId ? Number(deviceId) : null)
        }
      />

      <SelectorRow
        iconType="fan"
        label="Fan Device"
        subtitle="Actuator used for fan automation control"
        options={fanDevices}
        devices={devices}
        value={value?.fanDeviceId}
        expectedClass="ACTUATOR"
        expectedType="FAN"
        disabled={disabled}
        onChange={(deviceId) =>
          onChange?.("fanDeviceId", deviceId ? Number(deviceId) : null)
        }
      />

      <SelectorRow
        iconType="light"
        label="Light Device"
        subtitle="Actuator used for light automation control"
        options={lightDevices}
        devices={devices}
        value={value?.lightDeviceId}
        expectedClass="ACTUATOR"
        expectedType="LIGHT"
        disabled={disabled}
        onChange={(deviceId) =>
          onChange?.("lightDeviceId", deviceId ? Number(deviceId) : null)
        }
      />

      {isAdmin ? (
        <button
          type="button"
          className="config-device-add-row"
          disabled={disabled}
          onClick={() => onCreateDevice?.()}
        >
          <span className="config-device-add-row__icon">+</span>
          <span>Add New Device</span>
        </button>
      ) : null}

      <div className="config-selector-note">
        Each slot can select maximum 1 device. Do not assign the same device to multiple slots.
      </div>
    </div>
  );
}

function SelectorRow({
  iconType,
  label,
  subtitle,
  options = [],
  devices = [],
  value,
  expectedClass,
  expectedType,
  disabled = false,
  onChange,
}) {
  const selectedDevice = findDevice(devices, value);
  const selectedInOptions = options.some((device) => Number(device.id) === Number(value));
  const selectedInvalid = value != null && value !== "" && selectedDevice && !selectedInOptions;
  const selectedMissing = value != null && value !== "" && !selectedDevice;

  return (
    <div className="config-device-row">
      <div className="config-device-row__left">
        <div className={`config-device-row__icon ${iconType}`}>
          <DeviceIcon type={iconType} />
        </div>

        <div className="config-device-row__content">
          <div className="config-device-row__name">{label}</div>
          <div className="config-device-row__subtitle">{subtitle}</div>
        </div>
      </div>

      <div className="config-device-row__right">
        <select
          className="config-device-row__select"
          value={value ?? ""}
          disabled={disabled}
          onChange={(event) => onChange?.(event.target.value)}
        >
          <option value="">-- Select device --</option>
          {selectedInvalid ? (
            <option value={value}>
              Invalid for this slot: {getDeviceLabel(selectedDevice)}
            </option>
          ) : null}
          {selectedMissing ? (
            <option value={value}>
              Missing device #{value} - expected {expectedClass}/{expectedType}
            </option>
          ) : null}
          {options.map((device) => (
            <option key={device.id} value={device.id}>
              {getDeviceLabel(device)}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}

function DeviceIcon({ type }) {
  if (type === "humidity") {
    return (
      <svg viewBox="0 0 24 24" fill="none">
        <path
          d="M12 3C12 3 6 9.2 6 13.5A6 6 0 0 0 18 13.5C18 9.2 12 3 12 3Z"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    );
  }

  if (type === "light" || type === "light-sensor") {
    return (
      <svg viewBox="0 0 24 24" fill="none">
        <path
          d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.8 10.6c.7.6 1.3 1.5 1.5 2.4h4.6c.2-.9.8-1.8 1.5-2.4A6 6 0 0 0 12 3Z"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    );
  }

  if (type === "motion") {
    return (
      <svg viewBox="0 0 24 24" fill="none">
        <path
          d="M4 12a8 8 0 0 1 8-8"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
        />
        <path
          d="M4 18A14 14 0 0 1 18 4"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
        />
        <circle cx="12" cy="12" r="2.2" fill="currentColor" />
      </svg>
    );
  }

  if (type === "fan") {
    return (
      <svg viewBox="0 0 24 24" fill="none">
        <circle
          cx="12"
          cy="12"
          r="1.8"
          fill="currentColor"
        />
        <path
          d="M12 5.2c1.7 0 3 1.3 3 3 0 1-.4 1.8-1.2 2.5L12 12"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M17.8 13.2c0 1.7-1.3 3-3 3-1 0-1.8-.4-2.5-1.2L12 12"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M8.2 13.2c-1.7 0-3-1.3-3-3 0-1 .4-1.8 1.2-2.5L12 12"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" fill="none">
      <path
        d="M12 6V14"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path
        d="M9 9V14"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path
        d="M15 10V14"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path
        d="M8 18h8"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <rect
        x="7"
        y="4"
        width="10"
        height="14"
        rx="5"
        stroke="currentColor"
        strokeWidth="1.8"
      />
    </svg>
  );
}

export default MonitoringDeviceSelector;
