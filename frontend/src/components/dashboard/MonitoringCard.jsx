import "./MonitoringCard.css";

function MonitoringCard({ item }) {
  const statusClass = item?.status ? `monitoring-card--${item.status}` : "";
  console.log(item)
  return (
    <div className={`monitoring-card ${statusClass}`}>
      <div className="monitoring-card__info">
        <div className="monitoring-card__label">{item.label}</div>
        <div className="monitoring-card__value">{item.value}</div>
        <div className="monitoring-card__updated">{item.updatedText}</div>
      </div>

      <div className={`monitoring-card__icon ${item.icon || ""}`}>
        {renderMetricIcon(item.icon)}
      </div>
    </div>
  );
}

function renderMetricIcon(type) {
  switch (type) {
    case "temperature":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M12 14.5V6.2A2.2 2.2 0 1 1 16.4 6.2V14.5A4.2 4.2 0 1 1 12 14.5Z"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );

    case "humidity":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M13.2 4.2C13.2 4.2 8.3 9.6 8.3 12.7A4.9 4.9 0 0 0 18.1 12.7C18.1 9.6 13.2 4.2 13.2 4.2Z"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinejoin="round"
          />
          <path
            d="M7 10.4C7 10.4 4 13.5 4 15.5A3 3 0 0 0 10 15.5C10 13.5 7 10.4 7 10.4Z"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinejoin="round"
          />
        </svg>
      );

    case "light":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="3.6" stroke="currentColor" strokeWidth="1.8" />
          <path
            d="M12 2.5V5M12 19V21.5M21.5 12H19M5 12H2.5M18.7 5.3L17 7M7 17L5.3 18.7M18.7 18.7L17 17M7 7L5.3 5.3"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        </svg>
      );

    case "motion":
      return (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M2.5 12S6.2 5.5 12 5.5 21.5 12 21.5 12 17.8 18.5 12 18.5 2.5 12 2.5 12Z"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinejoin="round"
          />
          <circle cx="12" cy="12" r="2.7" stroke="currentColor" strokeWidth="1.8" />
        </svg>
      );

    default:
      return null;
  }
}

export default MonitoringCard;