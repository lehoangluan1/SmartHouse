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

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser());
  const [accessToken, setAccessToken] = useState(getStoredAccessToken());
  const [loading, setLoading] = useState(false);
  const [bootstrapping, setBootstrapping] = useState(true);

  useEffect(() => {
    async function bootstrap() {
      try {
        const storedRefreshToken = getStoredRefreshToken();
        if (!storedRefreshToken) {
          setBootstrapping(false);
          return;
        }

        const response = await refreshTokenApi(storedRefreshToken);
        const storedUser = getStoredUser();

        const session = {
          accessToken: response.accessToken || "",
          refreshToken: response.refreshToken || storedRefreshToken,
          userId: response.userId ?? storedUser?.userId ?? null,
          username: response.username || storedUser?.username || "",
          role: response.role || storedUser?.role || "",
          roleInHome: response.roleInHome || storedUser?.roleInHome || "",
          status: response.status || storedUser?.status || "",
          mustChangePassword:
            response.mustChangePassword ?? Boolean(storedUser?.mustChangePassword),
          homeId: response.homeId ?? storedUser?.homeId ?? null,
        };

        saveAuthSession(session);
        setAccessToken(session.accessToken);
        setUser({
          userId: session.userId,
          username: session.username,
          role: session.role,
          roleInHome: session.roleInHome,
          status: session.status,
          mustChangePassword: session.mustChangePassword,
          homeId: session.homeId,
        });
      } catch {
        clearAuthSession();
        setAccessToken("");
        setUser(null);
      } finally {
        setBootstrapping(false);
      }
    }

    bootstrap();
  }, []);

  async function loginWithLocal({ username, password }) {
    setLoading(true);
    try {
      const response = await loginLocal({ username, password });
      const session = normalizeSession(response);

      saveAuthSession(session);
      setAccessToken(session.accessToken);
      setUser({
        userId: session.userId,
        username: session.username,
        role: session.role,
        roleInHome: session.roleInHome,
        status: session.status,
        mustChangePassword: session.mustChangePassword,
        homeId: session.homeId,
      });

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
      setUser({
        userId: session.userId,
        username: session.username,
        role: session.role,
        roleInHome: session.roleInHome,
        status: session.status,
        mustChangePassword: session.mustChangePassword,
        homeId: session.homeId,
      });

      return session;
    } finally {
      setLoading(false);
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