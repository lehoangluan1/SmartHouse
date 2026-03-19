import "./AuthPageShell.css";

function GoogleIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="#EA4335"
        d="M12 10.2v3.9h5.4c-.23 1.26-.95 2.33-2.03 3.05l3.28 2.54c1.91-1.76 3.01-4.35 3.01-7.44 0-.72-.06-1.41-.18-2.06H12z"
      />
      <path
        fill="#34A853"
        d="M12 22c2.7 0 4.96-.89 6.61-2.41l-3.28-2.54c-.91.61-2.08.97-3.33.97-2.56 0-4.72-1.73-5.49-4.05H3.12v2.64A9.99 9.99 0 0 0 12 22z"
      />
      <path
        fill="#4A90E2"
        d="M6.51 13.97A5.99 5.99 0 0 1 6.2 12c0-.68.12-1.34.31-1.97V7.39H3.12A9.99 9.99 0 0 0 2 12c0 1.61.38 3.13 1.12 4.61l3.39-2.64z"
      />
      <path
        fill="#FBBC05"
        d="M12 5.98c1.47 0 2.79.51 3.83 1.5l2.87-2.87C16.95 2.98 14.69 2 12 2A9.99 9.99 0 0 0 3.12 7.39l3.39 2.64C7.28 7.71 9.44 5.98 12 5.98z"
      />
    </svg>
  );
}

function AuthProviderButtons({ onGoogleClick, disabled }) {
  return (
    <div className="auth-provider-buttons">
      <button
        type="button"
        className="auth-provider-buttons__google"
        onClick={onGoogleClick}
        disabled={disabled}
      >
        <span className="auth-provider-buttons__google-icon">
          <GoogleIcon />
        </span>
        <span>Continue with Google</span>
      </button>
    </div>
  );
}

export default AuthProviderButtons;