const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";
const AUTH_USER_KEY = "authUser";

export function saveAuthSession(payload) {
  if (!payload) return;

  localStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken || "");
  localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken || "");

  localStorage.setItem(
    AUTH_USER_KEY,
    JSON.stringify({
      userId: payload.userId ?? null,
      username: payload.username || "",
      role: payload.role || "",
      roleInHome: payload.roleInHome || "",
      status: payload.status || "",
      mustChangePassword: Boolean(payload.mustChangePassword),
      homeId: payload.homeId ?? null,
    })
  );
}

export function clearAuthSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
}

export function getStoredAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || "";
}

export function getStoredRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || "";
}

export function getStoredUser() {
  const raw = localStorage.getItem(AUTH_USER_KEY);
  if (!raw) return null;

  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}