import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import AuthHeader from "../../components/auth/AuthHeader";
import AuthPageShell from "../../components/auth/AuthPageShell";
import { linkCurrentGoogleAccount } from "../../api/accountAuthProviderApi";
import { useAuth } from "../../providers/AuthProvider";
import "./GoogleAuthCallbackPage.css";

function GoogleAuthCallbackPage() {
  const navigate = useNavigate();
  const { loginWithGoogle, logout } = useAuth();
  const [error, setError] = useState("");
  const handledRef = useRef(false);

  useEffect(() => {
    if (handledRef.current) return;
    handledRef.current = true;

    async function handleCallback() {
      try {
        const params = new URLSearchParams(window.location.search);
        const authorizationCode = params.get("code");
        const returnedError = params.get("error");
        const returnedState = params.get("state");

        const savedState = sessionStorage.getItem("google_oauth_state");
        const action = sessionStorage.getItem("google_oauth_action") || "login";

        const redirectUri =
          import.meta.env.VITE_GOOGLE_REDIRECT_URI ||
          `${window.location.origin}/auth/google/callback`;

        if (returnedError) {
          throw new Error("Google authentication was cancelled or failed");
        }

        if (!authorizationCode) {
          throw new Error("Missing authorization code from Google");
        }

        if (!savedState || !returnedState || returnedState !== savedState) {
          throw new Error("Invalid Google callback");
        }

        if (action === "link") {
          await linkCurrentGoogleAccount({
            authorizationCode,
            redirectUri,
          });

          sessionStorage.removeItem("google_oauth_state");
          sessionStorage.removeItem("google_oauth_action");

          navigate("/dashboard", {
            replace: true,
            state: { success: "Google account linked successfully" },
          });
          return;
        }

        await loginWithGoogle({
          authorizationCode,
          redirectUri,
        });

        sessionStorage.removeItem("google_oauth_state");
        sessionStorage.removeItem("google_oauth_action");

        navigate("/dashboard", { replace: true });
      } catch (err) {
        logout?.();
        setError(err.message || "Unable to process Google authentication");
      }
    }

    handleCallback();
  }, [loginWithGoogle, logout, navigate]);

  return (
    <AuthPageShell>
      <AuthCard>
        <AuthHeader
          title="Smart House"
          subtitle={error ? "Google authentication failed" : "Completing Google action..."}
        />

        {error ? (
          <div className="auth-error">
            {error}
            <div className="google-callback__actions">
              <button className="auth-submit" onClick={() => navigate("/login")}>
                Back to Login
              </button>
            </div>
          </div>
        ) : (
          <div className="google-callback__loading">Please wait...</div>
        )}
      </AuthCard>
    </AuthPageShell>
  );
}

export default GoogleAuthCallbackPage;