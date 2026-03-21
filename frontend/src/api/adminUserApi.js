import { request, unwrapObject } from "./apiClient";

const ADMIN_USERS_BASE = "/api/admin/users";

export async function createUserByAdmin(payload) {
  const result = await request(ADMIN_USERS_BASE, {
    method: "POST",
    body: payload,
  });

  return unwrapObject(result);
}

export async function fetchUserAuthProvidersByAdmin(userId) {
  const result = await request(`${ADMIN_USERS_BASE}/${userId}/auth-providers`);
  return unwrapObject(result);
}

export async function linkUserAuthProviderByAdmin(userId, payload) {
  const result = await request(`${ADMIN_USERS_BASE}/${userId}/auth-providers`, {
    method: "POST",
    body: payload,
  });

  return unwrapObject(result);
}