function ThresholdFieldRow({ label, value, unit, disabled = false, onChange }) {
    return (
      <label className="config-field-row">
        <span className="config-field-row__label">{label}</span>
        <input
          type="number"
          className="config-field-row__input"
          value={value ?? ""}
          disabled={disabled}
          onChange={(event) => onChange(Number(event.target.value))}
        />
        <span className="config-field-row__unit">{unit}</span>
      </label>
    );
  }
  
  export default ThresholdFieldRow;