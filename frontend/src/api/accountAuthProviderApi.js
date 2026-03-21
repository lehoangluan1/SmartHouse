import { request, unwrapObject } from "./apiClient";

const ACCOUNT_AUTH_PROVIDERS_BASE = "/api/account/auth-providers";

export async function fetchCurrentAuthProviders() {
  const result = await request(ACCOUNT_AUTH_PROVIDERS_BASE);
  return unwrapObject(result);
}

export async function linkCurrentGoogleAccount({
  authorizationCode,
  redirectUri,
}) {
  const result = await request(
    `${ACCOUNT_AUTH_PROVIDERS_BASE}/google/link`,
    {
      method: "POST",
      body: {
        authorizationCode,
        redirectUri,
      },
    }
  );

  return unwrapObject(result);
}