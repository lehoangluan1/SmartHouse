import "./../../pages/audit/AuditLogsPage.css";
import AuditIcon from "../../components/audit/AuditIcon";

function formatDateTime(value) {
  if (!value) {
    return { time: "-", date: "-" };
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return { time: "-", date: "-" };
  }

  return {
    time: date.toLocaleTimeString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
    }),
    date: date.toLocaleDateString("vi-VN"),
  };
}

function getEventTone(row) {
  if (row.category === "alerts") return "danger";
  if (row.category === "device") return "info";
  return "neutral";
}

function getStatusTone(status) {
  const value = String(status || "").toLowerCase();

  if (
    value.includes("failed") ||
    value.includes("error") ||
    value.includes("active")
  ) {
    return "danger";
  }

  if (
    value.includes("ack") ||
    value.includes("sent") ||
    value.includes("pending")
  ) {
    return "warning";
  }

  if (
    value.includes("resolved") ||
    value.includes("success") ||
    value.includes("logged")
  ) {
    return "success";
  }

  return "neutral";
}

function getEventIconName(type, category) {
  const value = String(type || "").toUpperCase();

  if (category === "alerts") return "alert";

  if (
    value.includes("POWER") ||
    value.includes("MANUAL") ||
    value.includes("CONTROL")
  ) {
    return "control";
  }

  if (
    value.includes("MODE") ||
    value.includes("CONFIG") ||
    value.includes("SETTING")
  ) {
    return "settings";
  }

  if (
    value.includes("DEVICE") ||
    value.includes("LIGHT") ||
    value.includes("FAN") ||
    value.includes("OFFLINE") ||
    value.includes("ONLINE")
  ) {
    return "device";
  }

  return "system";
}

function displayValue(value) {
  if (value === null || value === undefined || value === "") return "-";
  const text = String(value).trim();
  return text || "-";
}

function formatState(value) {
  const text = displayValue(value);
  if (text.toLowerCase() === "true") return "ON";
  if (text.toLowerCase() === "false") return "OFF";
  return text;
}

function formatCategoryLabel(category) {
  const value = String(category || "").toLowerCase();

  if (value === "alerts") return "Alert";
  if (value === "device") return "Device Event";
  if (value === "system") return "System Event";
  return "System Event";
}

function formatTypeLabel(type) {
  const value = displayValue(type);

  return value
    .toLowerCase()
    .split("_")
    .map((part) => (part ? part.charAt(0).toUpperCase() + part.slice(1) : ""))
    .join(" ");
}

function buildEventTitle(row) {
  const typeLabel = formatTypeLabel(row.type);
  const deviceLabel = displayValue(row.deviceLabel);
  const deviceName = displayValue(row.deviceName);

  if (row.category !== "device") {
    return typeLabel;
  }

  if (deviceLabel !== "-" && deviceLabel !== deviceName) {
    return `${deviceLabel} • ${typeLabel}`;
  }

  if (deviceName !== "-") {
    return `${deviceName} • ${typeLabel}`;
  }

  return typeLabel;
}

function buildDetails(row) {
  const details = displayValue(row.details);
  const deviceName = displayValue(row.deviceName);

  if (row.category === "device" && deviceName !== "-") {
    return `${details} • ${deviceName}`;
  }

  return details;
}

function EventHistoryTable({ rows = [] }) {
  return (
    <div className="audit-table-wrap">
      <table className="audit-table">
        <thead>
          <tr>
            <th>Event Type</th>
            <th>Time</th>
            <th>State Change</th>
            <th>Details</th>
            <th>Status</th>
          </tr>
        </thead>

        <tbody>
          {rows.length ? (
            rows.map((row, index) => {
              const { time, date } = formatDateTime(row.createdAt);
              const rowTone = getEventTone(row);
              const statusTone = getStatusTone(row.status);
              const iconName = getEventIconName(row.type, row.category);

              return (
                <tr key={`${row.source || "event"}-${row.id || index}`}>
                  <td>
                    <div className={`audit-event-type tone-${rowTone}`}>
                      <AuditIcon name={iconName} tone={rowTone} />

                      <div className="audit-event-type__content">
                        <span
                          className={`audit-event-type__category audit-event-type__category--${rowTone}`}
                        >
                          {formatCategoryLabel(row.category)}
                        </span>

                        <span
                          className="audit-event-type__name"
                          title={buildEventTitle(row)}
                        >
                          {buildEventTitle(row)}
                        </span>

                        {displayValue(row.deviceName) !== "-" &&
                        row.category === "device" ? (
                          <span
                            className="audit-event-type__device"
                            title={displayValue(row.deviceName)}
                          >
                            {displayValue(row.deviceName)}
                          </span>
                        ) : null}
                      </div>
                    </div>
                  </td>

                  <td>
                    <div className="audit-time-cell">
                      <strong>{time}</strong>
                      <span>{date}</span>
                    </div>
                  </td>

                  <td>
                    <div className="audit-state-change">
                      <span className="audit-badge audit-badge--soft">
                        {formatState(row.fromState)}
                      </span>
                      <span className="audit-state-change__arrow">→</span>
                      <span className="audit-badge audit-badge--danger">
                        {formatState(row.toState)}
                      </span>
                    </div>
                  </td>

                  <td
                    className="audit-details audit-cell-clamp audit-cell-clamp--details"
                    title={buildDetails(row)}
                  >
                    {buildDetails(row)}
                  </td>

                  <td>
                    <span className={`audit-status audit-status--${statusTone}`}>
                      {displayValue(row.status)}
                    </span>
                  </td>
                </tr>
              );
            })
          ) : (
            <tr>
              <td colSpan="5" className="audit-empty">
                No events found.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

export default EventHistoryTable;