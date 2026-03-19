
import "./AuthPageShell.css";
function AuthInput({
  label,
  type = "text",
  value,
  onChange,
  placeholder,
  autoComplete,
  disabled,
}) {
  return (
    <label className="auth-field">
      <span className="auth-field__label">{label}</span>
      <input
        className="auth-field__input"
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoComplete={autoComplete}
        disabled={disabled}
      />
    </label>
  );
}

export default AuthInput;