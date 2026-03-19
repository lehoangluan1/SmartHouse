import "./../../pages/audit/AuditLogsPage.css";

function AuditSearchBar({
  value,
  onChange,
  placeholder = "Search...",
  compact = false,
}) {
  return (
    <div className={`audit-search ${compact ? "is-compact" : ""}`}>
      <span className="audit-search__icon">⌕</span>
      <input
        type="text"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
      />
    </div>
  );
}

export default AuditSearchBar;