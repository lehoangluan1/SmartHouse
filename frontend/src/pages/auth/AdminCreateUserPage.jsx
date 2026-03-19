import { useMemo, useState } from "react";
import AuthCard from "../../components/auth/AuthCard";
import AuthHeader from "../../components/auth/AuthHeader";
import AuthInput from "../../components/auth/AuthInput";
import AuthPageShell from "../../components/auth/AuthPageShell";
import {
  createUserByAdmin,
  fetchUserAuthProvidersByAdmin,
  linkUserAuthProviderByAdmin,
} from "../../api/adminUserApi";
import "../../components/auth/AuthPageShell.css";
import "./AdminCreateUserPage.css";

const SYSTEM_ROLE_OPTIONS = ["ADMIN", "CUSTOMER", "INSTALLER"];
const PROVIDER_OPTIONS = ["LOCAL", "GOOGLE"];
const HOME_ROLE_OPTIONS = ["OWNER", "CO_OWNER", "RESIDENT", "GUEST", "TECHNICIAN", "VIEWER"];
const LINKABLE_PROVIDER_OPTIONS = ["GOOGLE"];

function AdminCreateUserPage() {
  const [username, setUsername] = useState("");
  const [provider, setProvider] = useState("LOCAL");
  const [systemRole, setSystemRole] = useState("CUSTOMER");
  const [homeMode, setHomeMode] = useState("CREATE_NEW");
  const [homeId, setHomeId] = useState("");
  const [homeName, setHomeName] = useState("");
  const [address, setAddress] = useState("");
  const [homeRole, setHomeRole] = useState("OWNER");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

  const [linkProvider, setLinkProvider] = useState("GOOGLE");
  const [linkProviderEmail, setLinkProviderEmail] = useState("");
  const [linkLoading, setLinkLoading] = useState(false);
  const [linkError, setLinkError] = useState("");
  const [linkedProviders, setLinkedProviders] = useState([]);

  const resolvedHomeRoleOptions = useMemo(() => {
    if (systemRole === "INSTALLER") {
      return ["TECHNICIAN", "VIEWER"];
    }

    if (homeMode === "CREATE_NEW") {
      return ["OWNER"];
    }

    return HOME_ROLE_OPTIONS.filter(
      (item) => item !== "TECHNICIAN" || systemRole === "INSTALLER"
    );
  }, [systemRole, homeMode]);

  async function loadLinkedProviders(userId) {
    const response = await fetchUserAuthProvidersByAdmin(userId);
    setLinkedProviders(response.providers || []);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setResult(null);
    setLinkedProviders([]);
    setLinkProvider("GOOGLE");
    setLinkProviderEmail("");
    setLinkError("");

    try {
      const trimmedUsername = username.trim();
      const trimmedHomeId = homeId.trim();
      const trimmedHomeName = homeName.trim();
      const trimmedAddress = address.trim();

      if (!trimmedUsername) {
        throw new Error("Username must not be blank");
      }

      if (homeMode === "JOIN_EXISTING" && !trimmedHomeId) {
        throw new Error("Enter home ID!");
      }

      if (homeMode === "CREATE_NEW" && !trimmedHomeName) {
        throw new Error("Enter Home Name!");
      }

      const payload = {
        username: trimmedUsername,
        provider,
        systemRole,
        homeAssignmentMode: homeMode,
        homeId: homeMode === "JOIN_EXISTING" ? Number(trimmedHomeId) : null,
        homeName: homeMode === "CREATE_NEW" ? trimmedHomeName : null,
        address: homeMode === "CREATE_NEW" ? (trimmedAddress || null) : null,
        homeRole: homeMode === "CREATE_NEW" ? "OWNER" : homeRole,
      };

      const response = await createUserByAdmin(payload);

      setResult(response);

      if (response?.id) {
        await loadLinkedProviders(response.id);
      }

      setUsername("");
      setProvider("LOCAL");
      setSystemRole("CUSTOMER");
      setHomeMode("CREATE_NEW");
      setHomeId("");
      setHomeName("");
      setAddress("");
      setHomeRole("OWNER");
    } catch (err) {
      setError(err.message || "Unable to create user");
    } finally {
      setLoading(false);
    }
  }

  async function handleLinkProvider(event) {
    event.preventDefault();

    if (!result?.id) {
      setLinkError("No user available to link");
      return;
    }

    setLinkLoading(true);
    setLinkError("");

    try {
      const trimmedProviderEmail = linkProviderEmail.trim().toLowerCase();

      if (!trimmedProviderEmail) {
        throw new Error("Provider email must not be blank");
      }

      const response = await linkUserAuthProviderByAdmin(result.id, {
        provider: linkProvider,
        providerEmail: trimmedProviderEmail,
      });

      setLinkedProviders(response.providers || []);
      setLinkProvider("GOOGLE");
      setLinkProviderEmail("");
    } catch (err) {
      setLinkError(err.message || "Unable to link provider");
    } finally {
      setLinkLoading(false);
    }
  }

  const isGoogleCreated = result?.provider === "GOOGLE";
  const canShowLinkSection = Boolean(result?.id);

  return (
    <AuthPageShell>
      <AuthCard className="admin-create-user-card">
        <AuthHeader title="Create User" />

        {error ? <div className="auth-error">{error}</div> : null}

        {result ? (
          <div className="auth-success">
            <div className="auth-result-grid">
              <div><strong>ID:</strong> {result.id}</div>
              <div><strong>Created:</strong> {result.username}</div>
              <div><strong>System Role:</strong> {result.role}</div>
              <div><strong>Home Role:</strong> {result.roleInHome}</div>
              <div><strong>Provider:</strong> {result.provider}</div>
              <div><strong>Status:</strong> {result.status}</div>
              <div><strong>Home ID:</strong> {result.homeId ?? "-"}</div>
              <div><strong>Home Name:</strong> {result.homeName ?? "-"}</div>
              <div><strong>Address:</strong> {result.address ?? "-"}</div>
              <div><strong>Must Change Password:</strong> {String(result.mustChangePassword)}</div>
              <div><strong>Temporary Password:</strong> {result.temporaryPassword || "-"}</div>
            </div>
          </div>
        ) : null}

        <form onSubmit={handleSubmit}>
          <AuthInput
            label="Username"
            value={username}
            onChange={setUsername}
            placeholder={provider === "GOOGLE" ? "name@gmail.com" : "username"}
            autoComplete="username"
            disabled={loading}
          />

          <label className="auth-field">
            <span className="auth-field__label">Provider</span>
            <select
              className="auth-field__input"
              value={provider}
              onChange={(e) => setProvider(e.target.value)}
              disabled={loading}
            >
              {PROVIDER_OPTIONS.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>

          <label className="auth-field">
            <span className="auth-field__label">System Role</span>
            <select
              className="auth-field__input"
              value={systemRole}
              onChange={(e) => {
                const nextRole = e.target.value;
                setSystemRole(nextRole);

                if (nextRole === "INSTALLER") {
                  setHomeRole("TECHNICIAN");
                  if (homeMode === "CREATE_NEW") {
                    setHomeMode("JOIN_EXISTING");
                  }
                } else if (homeMode === "CREATE_NEW") {
                  setHomeRole("OWNER");
                } else {
                  setHomeRole("RESIDENT");
                }
              }}
              disabled={loading}
            >
              {SYSTEM_ROLE_OPTIONS.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>

          <div className="auth-field">
              <span className="auth-field__label">Home Assignment</span>

              <div className="auth-radio-group">
                {systemRole !== "INSTALLER" ? (
                  <label
                    className={`auth-radio-card ${homeMode === "CREATE_NEW" ? "auth-radio-card--active" : ""}`}
                  >
                    <input
                      type="radio"
                      name="homeMode"
                      value="CREATE_NEW"
                      checked={homeMode === "CREATE_NEW"}
                      onChange={(e) => {
                        setHomeMode(e.target.value);
                        setHomeRole("OWNER");
                      }}
                      disabled={loading}
                    />
                    <div className="auth-radio-card__content">
                      <div className="auth-radio-card__title">Create New Home</div>
                      <div className="auth-radio-card__desc">Create a new home and assign this user as owner.</div>
                    </div>
                  </label>
                ) : null}

                <label
                  className={`auth-radio-card ${homeMode === "JOIN_EXISTING" ? "auth-radio-card--active" : ""}`}
                >
                  <input
                    type="radio"
                    name="homeMode"
                    value="JOIN_EXISTING"
                    checked={homeMode === "JOIN_EXISTING"}
                    onChange={(e) => {
                      setHomeMode(e.target.value);
                      setHomeRole(systemRole === "INSTALLER" ? "TECHNICIAN" : "RESIDENT");
                    }}
                    disabled={loading}
                  />
                  <div className="auth-radio-card__content">
                    <div className="auth-radio-card__title">Join Existing Home</div>
                    <div className="auth-radio-card__desc">Assign this user to an existing home by Home ID.</div>
                  </div>
                </label>
              </div>
            </div>

          {homeMode === "CREATE_NEW" ? (
            <>
              <AuthInput
                label="Home Name"
                value={homeName}
                onChange={setHomeName}
                placeholder="Enter name"
                disabled={loading}
              />

              <AuthInput
                label="Address"
                value={address}
                onChange={setAddress}
                placeholder="Enter your address"
                disabled={loading}
              />
            </>
          ) : (
            <AuthInput
              label="Home ID"
              value={homeId}
              onChange={setHomeId}
              placeholder="Enter your ID"
              disabled={loading}
            />
          )}

          <label className="auth-field">
            <span className="auth-field__label">Home Role</span>
            <select
              className="auth-field__input"
              value={homeMode === "CREATE_NEW" ? "OWNER" : homeRole}
              onChange={(e) => setHomeRole(e.target.value)}
              disabled={loading || homeMode === "CREATE_NEW"}
            >
              {(homeMode === "CREATE_NEW" ? ["OWNER"] : resolvedHomeRoleOptions).map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>

          <button className="auth-submit" type="submit" disabled={loading}>
            {loading ? "Creating..." : "Create User"}
          </button>
        </form>

        {canShowLinkSection ? (
          <div className="admin-link-provider-section">
            <div className="auth-divider">Linked Sign-in Methods</div>

            {linkedProviders.length ? (
              <div className="auth-success" style={{ marginBottom: 16 }}>
                <div className="auth-result-grid">
                  {linkedProviders.map((item, index) => (
                    <div key={`${item.provider}-${item.providerEmail}-${index}`}>
                      <strong>{item.provider}:</strong> {item.providerEmail}
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="auth-field__hint">User Provider Not Found.</div>
            )}

            {linkError ? <div className="auth-error">{linkError}</div> : null}

            {!isGoogleCreated ? (
              <form onSubmit={handleLinkProvider}>
                <label className="auth-field">
                  <span className="auth-field__label">Provider to Link</span>
                  <select
                    className="auth-field__input"
                    value={linkProvider}
                    onChange={(e) => setLinkProvider(e.target.value)}
                    disabled={linkLoading}
                  >
                    {LINKABLE_PROVIDER_OPTIONS.map((item) => (
                      <option key={item} value={item}>
                        {item}
                      </option>
                    ))}
                  </select>
                </label>

                <AuthInput
                  label="Google Email"
                  value={linkProviderEmail}
                  onChange={setLinkProviderEmail}
                  placeholder="name@gmail.com"
                  disabled={linkLoading}
                />

                <button className="auth-submit" type="submit" disabled={linkLoading}>
                  {linkLoading ? "Linking..." : "Link Google Account"}
                </button>
              </form>
            ) : (
              <div className="auth-field__hint">
                  User already link to GOOGLE.
              </div>
            )}
          </div>
        ) : null}
      </AuthCard>
    </AuthPageShell>
  );
}

export default AdminCreateUserPage;