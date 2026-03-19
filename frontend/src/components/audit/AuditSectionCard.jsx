import "./../../pages/audit/AuditLogsPage.css";

function AuditSectionCard({ title, children, className = "" }) {
  return (
    <section className={`audit-card ${className}`.trim()}>
      <div className="audit-card__header">
        <h3>{title}</h3>
      </div>
      <div className="audit-card__body">{children}</div>
    </section>
  );
}

export default AuditSectionCard;