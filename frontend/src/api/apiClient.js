import { dispatchLogoutEvent } from "../utils/authEvents";
import {
  getStoredAccessToken,
  getStoredRefreshToken,
  saveAuthSession,
  clearAuthSession,
  getStoredUser,
} from "../utils/authStorage";
import { refreshToken as refreshTokenApi } from "./authApi";

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.replace(/\/+$/, "") ||
  "http://localhost:8080";

export class ApiError extends Error {
  constructor(message, status, payload = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}

function getAccessToken() {
  return getStoredAccessToken() || "";
}

function normalizePath(path) {
  return path.startsWith("/") ? path : `/${path}`;
}

function buildUrl(path, query) {
  const url = new URL(`${API_BASE_URL}${normalizePath(path)}`);

  Object.entries(query || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    url.searchParams.append(key, String(value));
  });

  return url.toString();
}

async function parseJsonSafely(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

async function rawRequest(path, options = {}, tokenOverride = null) {
  const {
    method = "GET",
    query,
    body,
    auth = true,
    headers = {},
    signal,
  } = options;

  const token = tokenOverride ?? getAccessToken();
  const hasJsonBody = body !== undefined && body !== null;

  const response = await fetch(buildUrl(path, query), {
    method,
    signal,
    headers: {
      ...(hasJsonBody ? { "Content-Type": "application/json" } : {}),
      ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: hasJsonBody ? JSON.stringify(body) : undefined,
  });

  const json = await parseJsonSafely(response);
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
        accessToken: refreshed?.accessToken || "",
        refreshToken: refreshed?.refreshToken || refreshToken,
        userId: refreshed?.userId ?? storedUser?.userId ?? null,
        username: refreshed?.username || storedUser?.username || "",
        role: refreshed?.role || storedUser?.role || "",
        roleInHome: refreshed?.roleInHome || storedUser?.roleInHome || "",
        status: refreshed?.status || storedUser?.status || "",
        mustChangePassword:
          refreshed?.mustChangePassword ??
          Boolean(storedUser?.mustChangePassword),
        homeId: refreshed?.homeId ?? storedUser?.homeId ?? null,
      };

      saveAuthSession(session);
      return session.accessToken;
    })().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}

function resolveErrorMessage(status, payload) {
  return (
    payload?.message ||
    payload?.error ||
    payload?.details ||
    `Request failed with status ${status}`
  );
}

function handleUnauthorized() {
  clearAuthSession();
  dispatchLogoutEvent();
}

export async function request(path, options = {}) {
  const first = await rawRequest(path, options);

  if (first.response.ok) {
    return first.json;
  }

  if (first.response.status === 401 && options.auth !== false) {
    try {
      const newAccessToken = await refreshAccessToken();
      const retry = await rawRequest(path, options, newAccessToken);

      if (retry.response.ok) {
        return retry.json;
      }

      if (retry.response.status === 401) {
        handleUnauthorized();
      }

      throw new ApiError(
        resolveErrorMessage(retry.response.status, retry.json),
        retry.response.status,
        retry.json
      );
    } catch {
      handleUnauthorized();
      throw new ApiError("Session expired", 401, null);
    }
  }

  throw new ApiError(
    resolveErrorMessage(first.response.status, first.json),
    first.response.status,
    first.json
  );
}

export function unwrapData(result) {
  if (result == null) return null;
  if (typeof result === "object" && "data" in result) {
    return result.data ?? null;
  }
  return result;
}

export function unwrapObject(result) {
  const data = unwrapData(result);
  return data && !Array.isArray(data) ? data : null;
}

export function unwrapArray(result) {
  const data = unwrapData(result);
  return Array.isArray(data) ? data : [];
}

export function buildApiUrl(path, query) {
  return buildUrl(path, query);
}