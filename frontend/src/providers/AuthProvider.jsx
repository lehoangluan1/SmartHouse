import { createContext, useContext, useEffect, useMemo, useState } from "react";
import {
  loginGoogle,
  loginLocal,
  refreshToken as refreshTokenApi,
} from "../api/authApi";
import {
  clearAuthSession,
  getStoredAccessToken,
  getStoredRefreshToken,
  getStoredUser,
  saveAuthSession,
} from "../utils/authStorage";

const AuthContext = createContext(null);

let refreshPromise = null;

function normalizeSession(response) {
  if (!response) return null;

  return {
    accessToken: response.accessToken || "",
    refreshToken: response.refreshToken || "",
    userId: response.userId ?? null,
    username: response.username || "",
    role: response.role || "",
    roleInHome: response.roleInHome || "",
    status: response.status || "",
    mustChangePassword: Boolean(response.mustChangePassword),
    homeId: response.homeId ?? null,
  };
}

function buildUserFromSession(session) {
  if (!session) return null;

  return {
    userId: session.userId ?? null,
    username: session.username || "",
    role: session.role || "",
    roleInHome: session.roleInHome || "",
    status: session.status || "",
    mustChangePassword: Boolean(session.mustChangePassword),
    homeId: session.homeId ?? null,
  };
}

function parseJwt(token) {
  try {
    if (!token) return null;

    const parts = token.split(".");
    if (parts.length < 2) return null;

    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    const json = atob(padded);

    return JSON.parse(json);
  } catch {
    return null;
  }
}

function isTokenExpiredOrNearExpiry(token, bufferSeconds = 60) {
  if (!token) return true;

  const payload = parseJwt(token);
  if (!payload?.exp) return true;

  const now = Math.floor(Date.now() / 1000);
  return payload.exp - now <= bufferSeconds;
}

async function refreshTokenOnce(refreshToken) {
  if (!refreshToken) {
    throw new Error("Missing refresh token");
  }

  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = refreshTokenApi(refreshToken).finally(() => {
    refreshPromise = null;
  });

  return refreshPromise;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser());
  const [accessToken, setAccessToken] = useState(getStoredAccessToken());
  const [loading, setLoading] = useState(false);
  const [bootstrapping, setBootstrapping] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      try {
        const storedAccessToken = getStoredAccessToken();
        const storedRefreshToken = getStoredRefreshToken();
        const storedUser = getStoredUser();

        if (!storedRefreshToken) {
          if (!cancelled) {
            setAccessToken(storedAccessToken || "");
            setUser(storedUser);
            setBootstrapping(false);
          }
          return;
        }

        if (
          storedAccessToken &&
          !isTokenExpiredOrNearExpiry(storedAccessToken, 60)
        ) {
          if (!cancelled) {
            setAccessToken(storedAccessToken);
            setUser(storedUser);
            setBootstrapping(false);
          }
          return;
        }

        const response = await refreshTokenOnce(storedRefreshToken);

        const session = {
          accessToken: response?.accessToken || "",
          refreshToken: response?.refreshToken || storedRefreshToken,
          userId: response?.userId ?? storedUser?.userId ?? null,
          username: response?.username || storedUser?.username || "",
          role: response?.role || storedUser?.role || "",
          roleInHome: response?.roleInHome || storedUser?.roleInHome || "",
          status: response?.status || storedUser?.status || "",
          mustChangePassword:
            response?.mustChangePassword ??
            Boolean(storedUser?.mustChangePassword),
          homeId: response?.homeId ?? storedUser?.homeId ?? null,
        };

        saveAuthSession(session);

        if (!cancelled) {
          setAccessToken(session.accessToken);
          setUser(buildUserFromSession(session));
        }
      } catch (err) {
        const status = err?.response?.status;

        if (status === 401 || status === 403) {
          clearAuthSession();

          if (!cancelled) {
            setAccessToken("");
            setUser(null);
          }
        } else {
          const storedAccessToken = getStoredAccessToken();
          const storedUser = getStoredUser();

          if (!cancelled) {
            setAccessToken(storedAccessToken || "");
            setUser(storedUser);
          }
        }
      } finally {
        if (!cancelled) {
          setBootstrapping(false);
        }
      }
    }

    bootstrap();

    return () => {
      cancelled = true;
    };
  }, []);

  async function loginWithLocal({ username, password }) {
    setLoading(true);
    try {
      const response = await loginLocal({ username, password });
      const session = normalizeSession(response);

      saveAuthSession(session);
      setAccessToken(session.accessToken);
      setUser(buildUserFromSession(session));

      return session;
    } finally {
      setLoading(false);
    }
  }

  async function loginWithGoogle({ authorizationCode, redirectUri }) {
    setLoading(true);
    try {
      const response = await loginGoogle({ authorizationCode, redirectUri });
      const session = normalizeSession(response);

      saveAuthSession(session);
      setAccessToken(session.accessToken);
      setUser(buildUserFromSession(session));

      return session;
    } finally {
      setLoading(false);
    }
  }

  async function refreshAccessToken() {
    const storedRefreshToken = getStoredRefreshToken();
    const storedUser = getStoredUser();

    if (!storedRefreshToken) {
      clearAuthSession();
      setAccessToken("");
      setUser(null);
      throw new Error("Missing refresh token");
    }

    try {
      const response = await refreshTokenOnce(storedRefreshToken);

      const session = {
        accessToken: response?.accessToken || "",
        refreshToken: response?.refreshToken || storedRefreshToken,
        userId: response?.userId ?? storedUser?.userId ?? null,
        username: response?.username || storedUser?.username || "",
        role: response?.role || storedUser?.role || "",
        roleInHome: response?.roleInHome || storedUser?.roleInHome || "",
        status: response?.status || storedUser?.status || "",
        mustChangePassword:
          response?.mustChangePassword ??
          Boolean(storedUser?.mustChangePassword),
        homeId: response?.homeId ?? storedUser?.homeId ?? null,
      };

      saveAuthSession(session);
      setAccessToken(session.accessToken);
      setUser(buildUserFromSession(session));

      return session.accessToken;
    } catch (err) {
      const status = err?.response?.status;

      if (status === 401 || status === 403) {
        clearAuthSession();
        setAccessToken("");
        setUser(null);
      }

      throw err;
    }
  }

  function logout() {
    clearAuthSession();
    setAccessToken("");
    setUser(null);
  }

  const value = useMemo(
    () => ({
      user,
      accessToken,
      loading,
      bootstrapping,
      isAuthenticated: Boolean(accessToken),
      loginWithLocal,
      loginWithGoogle,
      refreshAccessToken,
      logout,
    }),
    [user, accessToken, loading, bootstrapping]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}