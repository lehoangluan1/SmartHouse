import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import AuthFooterNote from "../../components/auth/AuthFooterNote";
import AuthHeader from "../../components/auth/AuthHeader";
import AuthInput from "../../components/auth/AuthInput";
import AuthPageShell from "../../components/auth/AuthPageShell";
import AuthProviderButtons from "../../components/auth/AuthProviderButtons";
import { useAuth } from "../../providers/AuthProvider";
import "../../components/auth/AuthPageShell.css";

function buildGoogleAuthorizeUrl(action = "login") {
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || "";
  const redirectUri =
    import.meta.env.VITE_GOOGLE_REDIRECT_URI ||
    `${window.location.origin}/auth/google/callback`;

  const state = crypto.randomUUID();

  sessionStorage.setItem("google_oauth_state", state);
  sessionStorage.setItem("google_oauth_action", action);

  const url = new URL("https://accounts.google.com/o/oauth2/v2/auth");
  url.searchParams.set("client_id", clientId);
  url.searchParams.set("redirect_uri", redirectUri);
  url.searchParams.set("response_type", "code");
  url.searchParams.set("scope", "openid email profile");
  url.searchParams.set("access_type", "offline");
  url.searchParams.set("prompt", "consent");
  url.searchParams.set("state", state);

  return url.toString();
}

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { loginWithLocal, loading } = useAuth();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(location.state?.error || "");
  const [linkLoading, setLinkLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    try {
      const session = await loginWithLocal({
        username: username.trim(),
        password,
      });

      if (session.mustChangePassword) {
        navigate("/dashboard", { replace: true });
        return;
      }

      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err.message || "Login failed");
    }
  }

  function handleGoogleLogin() {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!clientId) {
      setError("Something went wrong");
      return;
    }

    window.location.href = buildGoogleAuthorizeUrl("login");
  }

  async function handleLinkGoogle() {
    setError("");
    setLinkLoading(true);

    try {
      const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
      if (!clientId) {
        throw new Error("Missing Google Client ID configuration");
      }

      await loginWithLocal({
        username: username.trim(),
        password,
      });

      window.location.href = buildGoogleAuthorizeUrl("link");
    } catch (err) {
      setError(err.message || "Unable to authenticate current account to link Google");
    } finally {
      setLinkLoading(false);
    }
  }

  return (
    <AuthPageShell>
      <AuthCard>
        <AuthHeader title="Smart House" />

        {error ? <div className="auth-error">{error}</div> : null}

        <form onSubmit={handleSubmit}>
          <AuthInput
            label="Username"
            value={username}
            onChange={setUsername}
            placeholder="Username"
            autoComplete="username"
            disabled={loading || linkLoading}
          />

          <AuthInput
            label="Password"
            type="password"
            value={password}
            onChange={setPassword}
            placeholder="••••••••"
            autoComplete="current-password"
            disabled={loading || linkLoading}
          />

          <button className="auth-submit" type="submit" disabled={loading || linkLoading}>
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>

        <div className="auth-divider">or</div>

        <AuthProviderButtons
          onGoogleClick={handleGoogleLogin}
          disabled={loading || linkLoading}
        />

        <button
          className="auth-submit"
          type="button"
          onClick={handleLinkGoogle}
          disabled={loading || linkLoading}
          style={{ marginTop: 12 }}
        >
          {linkLoading ? "Preparing Google link..." : "Link Current Account with Google"}
        </button>

        <AuthFooterNote />
      </AuthCard>
    </AuthPageShell>
  );
}

export default LoginPage;