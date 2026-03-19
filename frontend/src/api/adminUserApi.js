import { request, unwrapObject } from "./apiClient";

export async function createUserByAdmin(payload) {
  const result = await request("/api/admin/users", {
    method: "POST",
    body: payload,
  });

  return unwrapObject(result);
}

export async function fetchUserAuthProvidersByAdmin(userId) {
  const result = await request(`/api/admin/users/${userId}/auth-providers`, {
    method: "GET",
  });

  return unwrapObject(result);
}

export async function linkUserAuthProviderByAdmin(userId, payload) {
  const result = await request(`/api/admin/users/${userId}/auth-providers`, {
    method: "POST",
    body: payload,
  });

  return unwrapObject(result);
}