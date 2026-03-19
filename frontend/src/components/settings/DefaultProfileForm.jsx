import "./DefaultProfileForm.css";

const FIELDS = [
  { key: "tHigh", label: "T_high", suffix: "°C" },
  { key: "tLow", label: "T_low", suffix: "°C" },
  { key: "lLow", label: "L_low", suffix: "%" },
  { key: "lHigh", label: "L_high", suffix: "min" },
];

function DefaultProfileForm({ values, saving, onChange, onSave }) {
  return (
    <div className="default-profile-card">
      <div className="default-profile-card__grid">
        {FIELDS.map((field) => (
          <label key={field.key} className="default-profile-field">
            <span className="default-profile-field__label">{field.label}</span>

            <div className="default-profile-field__input-wrap">
              <input
                type="text"
                value={values[field.key] ?? ""}
                onChange={(event) => onChange(field.key, event.target.value)}
                className="default-profile-field__input"
              />
              <span className="default-profile-field__suffix">
                {field.suffix}
              </span>
            </div>
          </label>
        ))}
      </div>

      <div className="default-profile-card__actions">
        <button
          type="button"
          className="default-profile-card__save-btn"
          onClick={onSave}
          disabled={saving}
        >
          {saving ? "Saving..." : "Save Defaults"}
        </button>
      </div>
    </div>
  );
}

export default DefaultProfileForm;