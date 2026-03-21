import "./DeviceSwitchCard.css";

function DeviceSwitchCard({
  device,
  selected = false,
  disabled = false,
  onSelect,
  onToggle,
  onIntensityChange,
}) {
  const displayIntensity = Number.isFinite(Number(device.intensity))
    ? Math.max(0, Math.min(100, Number(device.intensity)))
    : 0;

  const sliderDisabled =
    disabled || !device.enabled || !device.supportsIntensity;

  return (
    <div
      className={`device-switch-card ${selected ? "selected" : ""}`}
      onClick={onSelect}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onSelect?.();
        }
      }}
    >
      <div className="device-switch-card__info">
        <div className="device-switch-card__name">{device.name}</div>
        <div className="device-switch-card__mode">{device.modeText}</div>

        {device.supportsIntensity ? (
          <div
            className="device-switch-card__intensity"
            onClick={(e) => e.stopPropagation()}
            onMouseDown={(e) => e.stopPropagation()}
            onPointerDown={(e) => e.stopPropagation()}
          >
            <div className="device-switch-card__intensity-head">
              <span>{device.intensityLabel}</span>
              <strong>{displayIntensity}%</strong>
            </div>

            <input
              type="range"
              min="0"
              max="100"
              step="1"
              value={displayIntensity}
              disabled={sliderDisabled}
              className="device-switch-card__slider"
              onChange={(e) => onIntensityChange?.(Number(e.target.value))}
            />
          </div>
        ) : null}
      </div>

      <div className="device-switch-card__actions">
        <button
          type="button"
          className={`device-switch-card__toggle ${device.enabled ? "on" : ""}`}
          disabled={disabled}
          onClick={(e) => {
            e.stopPropagation();
            onToggle?.();
          }}
        >
          {device.enabled ? (
            <span className="device-switch-card__on-text">ON</span>
          ) : null}
          <span className="device-switch-card__knob" />
        </button>

        <div
          className={`device-switch-card__icon ${device.icon} ${
            device.enabled ? "enabled" : "disabled"
          }`}
        >
          {renderDeviceIcon(device.icon)}
        </div>
      </div>
    </div>
  );
}

function renderDeviceIcon(type) {
  switch (type) {
    case "fan":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M12 12c-1.7-2.4-1.6-5.2-.5-7.2.5-.9 1.7-1 2.3-.1 1.2 1.8 1.5 4.8-1.8 7.3Z"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinejoin="round"
          />
          <path
            d="M12 12c2.9-.4 5.3-2 6.5-4 .6-.9 1.8-.8 2.2.2 1 2.2.1 5.1-2.8 6.1-1.6.5-3.8.4-5.9-2.3Z"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinejoin="round"
          />
          <path
            d="M12 12c1 2.8.7 5.6-.5 7.2-.6.8-1.8.7-2.3-.2-1.1-2-.9-5 1.6-7.1.9-.7 1.1-.7 1.2.1Z"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinejoin="round"
          />
          <circle cx="12" cy="12" r="1.3" fill="currentColor" />
        </svg>
      );
    case "bulb":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M9.1 17h5.8"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
          />
          <path
            d="M10.2 19.7h3.6"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
          />
          <path
            d="M8.8 14.1c-1-1-1.5-2.2-1.5-3.7 0-2.9 2.3-5.2 5.2-5.2 2.9 0 5.2 2.3 5.2 5.2 0 1.5-.6 2.8-1.6 3.8-.7.7-1.1 1.5-1.1 2.4H9.9c0-.9-.4-1.8-1.1-2.5Z"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinejoin="round"
          />
        </svg>
      );
    default:
      return null;
  }
}

export default DeviceSwitchCard;