import "./AuditIcon.css";

function AuditIcon({ name = "system", tone = "neutral" }) {
  return (
    <span className={`audit-icon audit-icon--${tone}`} aria-hidden="true">
      {name === "alert" ? (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M12 3 21 19H3L12 3Z"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinejoin="round"
          />
          <path
            d="M12 9V13"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
          <circle cx="12" cy="16.5" r="1" fill="currentColor" />
        </svg>
      ) : name === "device" ? (
        <svg viewBox="0 0 24 24" fill="none">
          <rect
            x="5"
            y="4"
            width="14"
            height="16"
            rx="3"
            stroke="currentColor"
            strokeWidth="1.8"
          />
          <path
            d="M9 8H15M9 12H15M9 16H13"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        </svg>
      ) : name === "settings" ? (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M12 8.5A3.5 3.5 0 1 0 12 15.5A3.5 3.5 0 1 0 12 8.5Z"
            stroke="currentColor"
            strokeWidth="1.8"
          />
          <path
            d="M19 12a7.7 7.7 0 0 0-.08-1l2-1.56-2-3.46-2.43.82a8.42 8.42 0 0 0-1.74-1L14.5 3h-5l-.25 2.8a8.42 8.42 0 0 0-1.74 1L5.08 6 3.08 9.46 5.08 11A8.7 8.7 0 0 0 5 12c0 .34.03.68.08 1l-2 1.56 2 3.46 2.43-.82c.53.42 1.11.76 1.74 1L9.5 21h5l.25-2.8c.63-.24 1.21-.58 1.74-1l2.43.82 2-3.46-2-1.56c.05-.32.08-.66.08-1Z"
            stroke="currentColor"
            strokeWidth="1.4"
            strokeLinejoin="round"
          />
        </svg>
      ) : name === "control" ? (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M12 3V12"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
          <path
            d="M7.05 5.8A8 8 0 1 0 16.95 5.8"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        </svg>
      ) : (
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M12 7V12L15 15"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth="1.8" />
        </svg>
      )}
    </span>
  );
}

export default AuditIcon;