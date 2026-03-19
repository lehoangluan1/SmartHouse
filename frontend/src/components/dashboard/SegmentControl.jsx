import "./SegmentControl.css";

function SegmentControl({
  options,
  activeValue,
  onChange,
  disabled = false,
  title = "System Mode",
}) {
  return (
    <div className="segment-control">
      <div className="segment-control__title">{title}</div>

      <div className="segment-control__group">
        {options.map((option) => {
          const isActive = option === activeValue;

          return (
            <button
              key={option}
              type="button"
              disabled={disabled}
              className={`segment-control__button ${isActive ? "active" : ""}`}
              onClick={() => onChange?.(option)}
            >
              {option}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export default SegmentControl;