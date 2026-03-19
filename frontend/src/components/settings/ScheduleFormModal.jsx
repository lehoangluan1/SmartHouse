import { useEffect, useMemo, useState } from "react";
import "./ScheduleFormModal.css";

const DAY_OPTIONS = [
  { label: "Mon", value: 0 },
  { label: "Tue", value: 1 },
  { label: "Wed", value: 2 },
  { label: "Thu", value: 3 },
  { label: "Fri", value: 4 },
  { label: "Sat", value: 5 },
  { label: "Sun", value: 6 },
];

const MODE_OPTIONS = ["AUTO", "MANUAL", "SLEEP", "AWAY"];

const EMPTY_FORM = {
  name: "",
  mode: "AUTO",
  startTime: "08:00",
  endTime: "18:00",
  enabled: true,
  days: [0, 1, 2, 3, 4, 5, 6],
};

function buildFormFromInitial(initialData) {
  if (!initialData) return EMPTY_FORM;

  return {
    name: initialData.name || "",
    mode: String(initialData.mode || "AUTO").toUpperCase(),
    startTime: formatTimeInput(initialData.startTime) || "08:00",
    endTime: formatTimeInput(initialData.endTime) || "18:00",
    enabled: initialData.enabled ?? true,
    days: parseDaysMask(initialData.daysMask),
  };
}

function parseDaysMask(daysMask) {
  if (daysMask == null || daysMask === 127) {
    return [0, 1, 2, 3, 4, 5, 6];
  }

  const result = [];
  for (let i = 0; i < 7; i += 1) {
    if ((daysMask & (1 << i)) !== 0) {
      result.push(i);
    }
  }
  return result;
}

function buildDaysMask(days) {
  return days.reduce((mask, day) => mask | (1 << day), 0);
}

function formatTimeInput(value) {
  if (!value) return "";
  return String(value).slice(0, 5);
}

function ScheduleFormModal({
  open,
  saving = false,
  initialData = null,
  onClose,
  onSubmit,
}) {
  const [form, setForm] = useState(EMPTY_FORM);
  const isEdit = useMemo(() => Boolean(initialData?.id), [initialData]);

  useEffect(() => {
    if (open) {
      setForm(buildFormFromInitial(initialData));
    }
  }, [open, initialData]);

  if (!open) return null;

  function handleChange(field, value) {
    setForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  }

  function handleToggleDay(dayValue) {
    setForm((prev) => {
      const exists = prev.days.includes(dayValue);
      const nextDays = exists
        ? prev.days.filter((item) => item !== dayValue)
        : [...prev.days, dayValue].sort((a, b) => a - b);

      return {
        ...prev,
        days: nextDays,
      };
    });
  }

  function handleSelectAllDays() {
    setForm((prev) => ({
      ...prev,
      days: [0, 1, 2, 3, 4, 5, 6],
    }));
  }

  function handleClearDays() {
    setForm((prev) => ({
      ...prev,
      days: [],
    }));
  }

  function validateForm() {
    if (!form.startTime) {
        alert("Please select start time");
        return false;
    }

    if (!form.endTime) {
        alert("Please select end time");
        return false;
    }

    if (form.startTime === form.endTime) {
        alert("Start time and end time cannot be the same");
        return false;
    }

    if (!form.mode) {
        alert("Please select mode");
        return false;
    }

    if (!form.days.length) {
        alert("Please select at least one day");
        return false;
    }

    return true;
    }

  function handleSubmit(event) {
    event.preventDefault();

    if (!validateForm()) return;

    const payload = {
      name: form.name.trim(),
      mode: form.mode,
      startTime: form.startTime,
      endTime: form.endTime,
      daysMask: buildDaysMask(form.days),
      enabled: form.enabled,
    };

    onSubmit?.(payload);
  }

  return (
    <div className="schedule-modal">
      <div className="schedule-modal__backdrop" onClick={saving ? undefined : onClose} />

      <div className="schedule-modal__dialog" role="dialog" aria-modal="true">
        <div className="schedule-modal__header">
          <div>
            <h3 className="schedule-modal__title">
              {isEdit ? "Edit Schedule" : "Create Schedule"}
            </h3>
          </div>

          <button
            type="button"
            className="schedule-modal__close"
            onClick={onClose}
            disabled={saving}
            aria-label="Close"
          >
            ×
          </button>
        </div>

        <form className="schedule-form" onSubmit={handleSubmit}>
          <div className="schedule-form__grid">
            <div className="schedule-form__field schedule-form__field--full">
              <label className="schedule-form__label">Schedule name</label>
              <input
                type="text"
                className="schedule-form__input"
                placeholder="Ví dụ: Morning Auto"
                value={form.name}
                onChange={(e) => handleChange("name", e.target.value)}
              />
            </div>

            <div className="schedule-form__field">
              <label className="schedule-form__label">Mode</label>
              <select
                className="schedule-form__input"
                value={form.mode}
                onChange={(e) => handleChange("mode", e.target.value)}
              >
                {MODE_OPTIONS.map((mode) => (
                  <option key={mode} value={mode}>
                    {mode}
                  </option>
                ))}
              </select>
            </div>

            <div className="schedule-form__field schedule-form__field--switch">
              <label className="schedule-form__label">Enabled</label>
              <label className="schedule-form__toggle">
                <input
                  type="checkbox"
                  checked={form.enabled}
                  onChange={(e) => handleChange("enabled", e.target.checked)}
                />
                <span className="schedule-form__toggle-ui" />
              </label>
            </div>

            <div className="schedule-form__field">
              <label className="schedule-form__label">Start time</label>
              <input
                type="time"
                className="schedule-form__input"
                value={form.startTime}
                onChange={(e) => handleChange("startTime", e.target.value)}
              />
            </div>

            <div className="schedule-form__field">
              <label className="schedule-form__label">End time</label>
              <input
                type="time"
                className="schedule-form__input"
                value={form.endTime}
                onChange={(e) => handleChange("endTime", e.target.value)}
              />
            </div>

            <div className="schedule-form__field schedule-form__field--full">
              <div className="schedule-form__days-head">
                <label className="schedule-form__label">Days</label>

                <div className="schedule-form__days-actions">
                  <button
                    type="button"
                    className="schedule-form__text-button"
                    onClick={handleSelectAllDays}
                    disabled={saving}
                  >
                    All
                  </button>
                  <button
                    type="button"
                    className="schedule-form__text-button"
                    onClick={handleClearDays}
                    disabled={saving}
                  >
                    Clear
                  </button>
                </div>
              </div>

              <div className="schedule-form__days">
                {DAY_OPTIONS.map((day) => {
                  const active = form.days.includes(day.value);

                  return (
                    <button
                      key={day.value}
                      type="button"
                      className={`schedule-form__day ${active ? "is-active" : ""}`}
                      onClick={() => handleToggleDay(day.value)}
                      disabled={saving}
                    >
                      {day.label}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          <div className="schedule-form__footer">
            <button
              type="button"
              className="schedule-form__secondary"
              onClick={onClose}
              disabled={saving}
            >
              Cancel
            </button>

            <button
              type="submit"
              className="schedule-form__primary"
              disabled={saving}
            >
              {saving ? "Saving..." : isEdit ? "Save Changes" : "Create Schedule"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ScheduleFormModal;