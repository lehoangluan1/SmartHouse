import { useEffect, useState } from "react";

const DEFAULT_FORM = {
  name: "",
  subtype: "TEMPERATURE_NODE",
  roomName: "",
  deviceKey: "",
};

const DEVICE_TYPE_OPTIONS = [
  { value: "TEMPERATURE_NODE", label: "Temperature Sensor" },
  { value: "HUMIDITY_NODE", label: "Humidity Sensor" },
  { value: "LIGHT_NODE", label: "Light Sensor" },
  { value: "MOTION_NODE", label: "Motion Sensor" },
];

function CreateDeviceModal({
  open,
  saving = false,
  error = "",
  onClose,
  onSubmit,
}) {
  const [form, setForm] = useState(DEFAULT_FORM);

  useEffect(() => {
    if (open) {
      setForm(DEFAULT_FORM);
    }
  }, [open]);

  if (!open) return null;

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();

    onSubmit?.({
      name: form.name.trim(),
      subtype: form.subtype,
      roomName: form.roomName.trim() || null,
      deviceKey: form.deviceKey.trim(),
    });
  }

  return (
    <div className="device-modal" role="dialog" aria-modal="true">
      <div
        className="device-modal__backdrop"
        onClick={saving ? undefined : onClose}
      />

      <div className="device-modal__dialog">
        <div className="device-modal__header">
          <div className="device-modal__header-content">
            <h3>Add New Device</h3>
            <p>Create a new monitoring device for this config.</p>
          </div>

          <button
            type="button"
            className="device-modal__close"
            onClick={onClose}
            disabled={saving}
            aria-label="Close"
          >
            ×
          </button>
        </div>

        <form className="device-form" onSubmit={handleSubmit}>
          <div className="device-form__grid">
            <label className="device-form__field">
              <span>Device Name</span>
              <input
                type="text"
                value={form.name}
                disabled={saving}
                onChange={(e) => updateField("name", e.target.value)}
                placeholder="My Room Sensor"
              />
            </label>

            <label className="device-form__field">
              <span>Device Type</span>
              <select
                value={form.subtype}
                disabled={saving}
                onChange={(e) => updateField("subtype", e.target.value)}
              >
                {DEVICE_TYPE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="device-form__field">
              <span>Room</span>
              <input
                type="text"
                value={form.roomName}
                disabled={saving}
                onChange={(e) => updateField("roomName", e.target.value)}
                placeholder="Bedroom"
              />
            </label>

            <label className="device-form__field">
              <span>Device ID / Key</span>
              <input
                type="text"
                value={form.deviceKey}
                disabled={saving}
                onChange={(e) => updateField("deviceKey", e.target.value)}
                placeholder="temp-bedroom-001"
              />
            </label>
          </div>

          {error ? <div className="device-form__error">{error}</div> : null}

          <div className="device-form__actions">
            <button
              type="button"
              className="device-form__button device-form__button--secondary"
              disabled={saving}
              onClick={onClose}
            >
              Cancel
            </button>

            <button
              type="submit"
              className="device-form__button device-form__button--primary"
              disabled={saving}
            >
              {saving ? "Creating..." : "Create Device"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreateDeviceModal;