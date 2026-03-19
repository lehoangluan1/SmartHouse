import "./../../pages/audit/AuditLogsPage.css";

function formatConfigSnapshot(raw) {
  if (!raw || raw === "-") return "-";

  try {
    const value = typeof raw === "string" ? JSON.parse(raw) : raw;
    if (!value) return "-";

    if (typeof value !== "object") return String(value);

    const name = value.name || `Config #${value.id || "?"}`;
    const active = value.isActive ? " (active)" : "";
    return `${name}${active}`;
  } catch {
    return raw;
  }
}
function formatReason(raw) {
  if (!raw || raw === "-") return "-";

  try {
    const value = typeof raw === "string" ? JSON.parse(raw) : raw;
    if (!value) return "-";

    if (typeof value === "object") {
      return value.message || value.reason || value.action || JSON.stringify(value);
    }

    return String(value);
  } catch {
    return raw;
  }
}

function ConfigChangesTable({ rows = [] }) {
  return (
    <div className="audit-table-wrap audit-table-wrap--left">
      <table className="audit-table audit-table--config">
        <thead>
          <tr>
            <th>User</th>
            <th>Time</th>
            <th>Prev</th>
            <th>New Config</th>
            <th>Reason</th>
          </tr>
        </thead>

        <tbody>
          {rows.length ? (
            rows.map((row) => (
              <tr key={row.id}>
                <td className="audit-user-cell">{row.user}</td>

                <td>
                  <div className="audit-time-cell">
                    <strong>{row.time}</strong>
                    <span>{row.date}</span>
                  </div>
                </td>

                <td
                  className="audit-muted audit-cell-clamp"
                  title={formatConfigSnapshot(row.prevConfig)}
                >
                  {formatConfigSnapshot(row.prevConfig)}
                </td>

                <td
                  className="audit-linkish audit-cell-clamp"
                  title={formatConfigSnapshot(row.newConfig)}
                >
                  {formatConfigSnapshot(row.newConfig)}
                </td>

                <td
                  className="audit-muted audit-cell-clamp"
                  title={formatReason(row.reason)}
                >
                  {formatReason(row.reason)}
                </td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="5" className="audit-empty">
                No config changes found.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

export default ConfigChangesTable;