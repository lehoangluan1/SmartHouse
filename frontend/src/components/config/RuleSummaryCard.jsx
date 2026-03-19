function RuleSummaryCard({ thresholds = {} }) {
  return (
    <aside className="config-rule-summary">
      <div className="config-rule-summary__title">Rule summary</div>

      <ul>
        <li>
          <strong>Auto:</strong> Fan on when Temp &gt; {thresholds.tHigh}°C, off when Temp &lt;{" "}
          {thresholds.tLow}°C, fan speed {thresholds.autoFanSpeed}%
        </li>

        <li>
          <strong>Sleep:</strong> Maintain Temp from {thresholds.tSleepLow}°C to{" "}
          {thresholds.tSleepHigh}°C, fan speed {thresholds.sleepFanSpeed}%
        </li>

        <li>
          <strong>Away:</strong> Cool when Temp &gt; {thresholds.tAwayHigh}°C, fan speed{" "}
          {thresholds.awayFanSpeed}%, trigger after {thresholds.dPresent} min no presence
        </li>

        <li>
          <strong>Light:</strong> Turn on when Light &lt; {thresholds.lLow}%, turn off when Light &gt;{" "}
          {thresholds.lHigh}%
        </li>

        <li>
          <strong>Alert:</strong> Trigger if Temp &gt; {thresholds.tCritical}°C for {thresholds.n} min
        </li>

        <li>
          <strong>Offline:</strong> Alert after {thresholds.m} min
        </li>

        <li>
          <strong>Manual hold:</strong> Keep manual state for {thresholds.tHold} min
        </li>

        <li>
          <strong>Energy save:</strong> Apply after {thresholds.k} min
        </li>
      </ul>
    </aside>
  );
}

export default RuleSummaryCard;