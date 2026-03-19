import { dispatchLogoutEvent } from "../utils/authEvents";
import {
  getStoredAccessToken,
  getStoredRefreshToken,
  saveAuthSession,
  clearAuthSession,
  getStoredUser,
} from "../utils/authStorage";
import { refreshToken as refreshTokenApi } from "./authApi";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.replace(/\/+$/, "") || "http://localhost:8080";

export class ApiError extends Error {
  constructor(message, status, payload = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}

function getToken() {
  return getStoredAccessToken() || "";
}

function buildUrl(path, query) {
  const url = new URL(`${API_BASE_URL}${path}`);

  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.append(key, value);
    }
  });

  return url.toString();
}

async function doFetch(path, options = {}, tokenOverride = null) {
  const { method = "GET", query, body, auth = true, headers = {} } = options;

  const token = tokenOverride ?? getToken();

  const response = await fetch(buildUrl(path, query), {
    method,
    headers: {
      ...(body ? { "Content-Type": "application/json" } : {}),
      ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const json = await response.json().catch(() => null);

  return { response, json };
}

let refreshPromise = null;

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = getStoredRefreshToken();
      const storedUser = getStoredUser();

      if (!refreshToken) {
        throw new Error("Missing refresh token");
      }

      const refreshed = await refreshTokenApi(refreshToken);

      const session = {
        accessToken: refreshed.accessToken || "",
        refreshToken: refreshed.refreshToken || refreshToken,
        userId: refreshed.userId ?? storedUser?.userId ?? null,
        username: refreshed.username || storedUser?.username || "",
        role: refreshed.role || storedUser?.role || "",
        roleInHome: refreshed.roleInHome || storedUser?.roleInHome || "",
        status: refreshed.status || storedUser?.status || "",
        mustChangePassword:
          refreshed.mustChangePassword ?? Boolean(storedUser?.mustChangePassword),
        homeId: refreshed.homeId ?? storedUser?.homeId ?? null,
      };

      saveAuthSession(session);
      return session.accessToken;
    })().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}

export async function request(path, options = {}) {
  const first = await doFetch(path, options);

  if (first.response.ok) {
    return first.json;
  }

  if (first.response.status === 401 && options.auth !== false) {
    try {
      const newAccessToken = await refreshAccessToken();
      const retry = await doFetch(path, options, newAccessToken);

      if (retry.response.ok) {
        return retry.json;
      }

      const retryMessage =
        retry.json?.message ||
        retry.json?.error ||
        retry.json?.details ||
        `Request failed with status ${retry.response.status}`;

      if (retry.response.status === 401) {
        clearAuthSession();
        dispatchLogoutEvent();
      }

      throw new ApiError(retryMessage, retry.response.status, retry.json);
    } catch {
      clearAuthSession();
      dispatchLogoutEvent();
      throw new ApiError("Session expired", 401, null);
    }
  }

  const message =
    first.json?.message ||
    first.json?.error ||
    first.json?.details ||
    `Request failed with status ${first.response.status}`;

  throw new ApiError(message, first.response.status, first.json);
}

export function unwrapData(result) {
  if (result == null) return null;
  if (typeof result === "object" && "data" in result) {
    return result.data ?? null;
  }
  return result;
}

export function unwrapArray(result) {
  const data = unwrapData(result);
  return Array.isArray(data) ? data : [];
}

export function unwrapObject(result) {
  const data = unwrapData(result);
  return Array.isArray(data) ? null : data ?? null;
}

export { API_BASE_URL };