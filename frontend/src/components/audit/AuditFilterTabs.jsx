import "./../../pages/audit/AuditLogsPage.css";

function AuditFilterTabs({ tabs, activeTab, onChange }) {
  return (
    <div className="audit-tabs">
      {tabs.map((tab) => (
        <button
          key={tab.value}
          type="button"
          className={tab.value === activeTab ? "active" : ""}
          onClick={() => onChange(tab.value)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}

export default AuditFilterTabs;