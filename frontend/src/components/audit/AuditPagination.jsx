import "./../../pages/audit/AuditLogsPage.css";

function AuditPagination({
  page = 0,
  totalPages = 0,
  onChange,
}) {
  if (totalPages <= 1) return null;

  return (
    <div className="audit-pagination">
      <button
        type="button"
        className="audit-pagination__button"
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
      >
        Prev
      </button>

      <span className="audit-pagination__info">
        Page <strong>{page + 1}</strong> / {totalPages}
      </span>

      <button
        type="button"
        className="audit-pagination__button"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next
      </button>
    </div>
  );
}

export default AuditPagination;