import "./DeviceFilterBar.css";

function DeviceFilterBar({ title, options, selectedValues, onSelect }) {
  return (
    <div className="device-filter-bar">
      <div className="device-filter-bar__title">{title}</div>

      <div className="device-filter-bar__list">
        {options.map((option) => {
          const isGroup =
            option.value === "ALL" ||
            option.value === "SENSORS" ||
            option.value === "ACTUATORS";

          const active = isGroup
            ? isGroupActive(option.value, selectedValues)
            : selectedValues.includes(option.value);

          return (
            <button
              key={option.value}
              type="button"
              className={`device-filter-chip ${
                active ? "active" : ""
              } ${option.tone || "neutral"}`}
              onClick={() => onSelect?.(option.value)}
            >
              <span>{option.label}</span>
              {active && !isGroup ? <span className="device-filter-chip__check">✓</span> : null}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function isGroupActive(value, selectedValues) {
  if (value === "ALL") {
    return (
      selectedValues.includes("TEMPERATURE_NODE") &&
      selectedValues.includes("HUMIDITY_NODE") &&
      selectedValues.includes("LIGHT_NODE") &&
      selectedValues.includes("FAN") &&
      selectedValues.includes("LIGHT")
    );
  }

  if (value === "SENSORS") {
    return (
      selectedValues.length === 3 &&
      selectedValues.includes("TEMPERATURE_NODE") &&
      selectedValues.includes("HUMIDITY_NODE") &&
      selectedValues.includes("LIGHT_NODE")
    );
  }

  if (value === "ACTUATORS") {
    return (
      selectedValues.length === 2 &&
      selectedValues.includes("FAN") &&
      selectedValues.includes("LIGHT")
    );
  }

  return false;
}

export default DeviceFilterBar;