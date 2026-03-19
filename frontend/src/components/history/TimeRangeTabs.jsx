import "./TimeRangeTabs.css";

function TimeRangeTabs({ title, options, activeValue, onChange }) {
  return (
    <div className="time-range-tabs">
      <div className="time-range-tabs__title">{title}</div>

      <div className="time-range-tabs__list">
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            className={`time-range-tabs__item ${
              option.value === activeValue ? "active" : ""
            }`}
            onClick={() => onChange?.(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}

export default TimeRangeTabs;