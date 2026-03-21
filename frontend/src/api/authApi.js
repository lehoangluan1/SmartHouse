import { request, unwrapObject } from "./apiClient";

const AUTH_BASE = "/api/auth";

export async function loginLocal(payload) {
  const result = await request(`${AUTH_BASE}/login`, {
    method: "POST",
    auth: false,
    body: {
      provider: "LOCAL",
      username: payload.username,
      password: payload.password,
      authorizationCode: null,
      redirectUri: null,
    },
  });

  return unwrapObject(result);
}

export async function loginGoogle(payload) {
  const result = await request(`${AUTH_BASE}/login`, {
    method: "POST",
    auth: false,
    body: {
      provider: "GOOGLE",
      username: null,
      password: null,
      authorizationCode: payload.authorizationCode,
      redirectUri: payload.redirectUri,
    },
  });

  return unwrapObject(result);
}

export async function refreshToken(refreshToken) {
  const result = await request(`${AUTH_BASE}/refresh`, {
    method: "POST",
    auth: false,
    body: { refreshToken },
  });

  return unwrapObject(result);
}