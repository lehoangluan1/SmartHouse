import { request, unwrapObject } from "./apiClient";

export async function fetchCurrentAuthProviders() {
  const result = await request("/api/account/auth-providers", {
    method: "GET",
  });

  return unwrapObject(result);
}

export async function linkCurrentGoogleAccount({ authorizationCode, redirectUri }) {
  const result = await request("/api/account/auth-providers/google/link", {
    method: "POST",
    body: {
      authorizationCode,
      redirectUri,
    },
  });

  return unwrapObject(result);
}