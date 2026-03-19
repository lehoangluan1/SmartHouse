import "./AuthPageShell.css";

function AuthPageShell({ children }) {
  return (
    <div className="auth-shell">
      <div className="auth-shell__overlay" />
      <div className="auth-shell__content">{children}</div>
    </div>
  );
}

export default AuthPageShell;