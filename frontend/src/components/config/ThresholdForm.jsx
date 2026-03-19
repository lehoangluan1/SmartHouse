import ThresholdFieldRow from "./ThresholdFieldRow";

function ThresholdForm({ values, disabled = false, onChange }) {
  return (
    <div className="config-thresholds__form">
      <ThresholdFieldRow
        label="T_high:"
        value={values.tHigh}
        unit="°C"
        disabled={disabled}
        onChange={(value) => onChange("tHigh", value)}
      />

      <ThresholdFieldRow
        label="T_low:"
        value={values.tLow}
        unit="°C"
        disabled={disabled}
        onChange={(value) => onChange("tLow", value)}
      />

      <ThresholdFieldRow
        label="L_low:"
        value={values.lLow}
        unit="%"
        disabled={disabled}
        onChange={(value) => onChange("lLow", value)}
      />

      <ThresholdFieldRow
        label="L_high:"
        value={values.lHigh}
        unit="%"
        disabled={disabled}
        onChange={(value) => onChange("lHigh", value)}
      />

      <ThresholdFieldRow
        label="T_sleep_high:"
        value={values.tSleepHigh}
        unit="°C"
        disabled={disabled}
        onChange={(value) => onChange("tSleepHigh", value)}
      />

      <ThresholdFieldRow
        label="T_sleep_low:"
        value={values.tSleepLow}
        unit="°C"
        disabled={disabled}
        onChange={(value) => onChange("tSleepLow", value)}
      />

      <ThresholdFieldRow
        label="T_away_high:"
        value={values.tAwayHigh}
        unit="°C"
        disabled={disabled}
        onChange={(value) => onChange("tAwayHigh", value)}
      />

      <ThresholdFieldRow
        label="T_critical:"
        value={values.tCritical}
        unit="°C"
        disabled={disabled}
        onChange={(value) => onChange("tCritical", value)}
      />

      <ThresholdFieldRow
        label="N:"
        value={values.n}
        unit="min"
        disabled={disabled}
        onChange={(value) => onChange("n", value)}
      />

      <ThresholdFieldRow
        label="M:"
        value={values.m}
        unit="min"
        disabled={disabled}
        onChange={(value) => onChange("m", value)}
      />

      <ThresholdFieldRow
        label="T_hold:"
        value={values.tHold}
        unit="min"
        disabled={disabled}
        onChange={(value) => onChange("tHold", value)}
      />

      <ThresholdFieldRow
        label="D_present:"
        value={values.dPresent}
        unit="min"
        disabled={disabled}
        onChange={(value) => onChange("dPresent", value)}
      />

      <ThresholdFieldRow
        label="K:"
        value={values.k}
        unit="min"
        disabled={disabled}
        onChange={(value) => onChange("k", value)}
      />

      <ThresholdFieldRow
        label="Auto fan speed:"
        value={values.autoFanSpeed}
        unit="%"
        disabled={disabled}
        onChange={(value) => onChange("autoFanSpeed", value)}
      />

      <ThresholdFieldRow
        label="Sleep fan speed:"
        value={values.sleepFanSpeed}
        unit="%"
        disabled={disabled}
        onChange={(value) => onChange("sleepFanSpeed", value)}
      />

      <ThresholdFieldRow
        label="Away fan speed:"
        value={values.awayFanSpeed}
        unit="%"
        disabled={disabled}
        onChange={(value) => onChange("awayFanSpeed", value)}
      />
    </div>
  );
}

export default ThresholdForm;