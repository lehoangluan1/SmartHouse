import { request, unwrapObject } from "./apiClient";

const HOMES_BASE = "/api/homes";
const HOME_PROFILES_BASE = "/api/home-profiles";

export async function fetchHomeUsers(homeId) {
  const result = await request(`${HOMES_BASE}/${homeId}/users`);
  return unwrapObject(result);
}

export async function activateHomeProfile(homeId) {
  const result = await request(`${HOME_PROFILES_BASE}/${homeId}/activate`, {
    method: "POST",
  });
  return unwrapObject(result);
}

export async function addHomeUser(homeId, payload) {
  const result = await request(`${HOMES_BASE}/${homeId}/users`, {
    method: "POST",
    body: payload,
  });
  return unwrapObject(result);
}

export async function updateHomeUser(homeId, userId, payload) {
  const result = await request(`${HOMES_BASE}/${homeId}/users/${userId}`, {
    method: "PATCH",
    body: payload,
  });
  return unwrapObject(result);
}

export async function removeHomeUser(homeId, userId) {
  const result = await request(`${HOMES_BASE}/${homeId}/users/${userId}`, {
    method: "DELETE",
  });
  return unwrapObject(result);
}

export async function setHomeUserPassword(homeId, userId, payload) {
  const result = await request(
    `${HOMES_BASE}/${homeId}/users/${userId}/set-password`,
    {
      method: "POST",
      body: payload,
    }
  );
  return unwrapObject(result);
}