import { request, unwrapObject } from "./apiClient";

export async function fetchHomeUsers(homeId) {
  const result = await request(`/api/homes/${homeId}/users`, {
    method: "GET",
  });
  return unwrapObject(result);
}

export async function activateHomeProfile(homeId) {
  const result = await request(`/api/home-profiles/${homeId}/activate`, {
    method: "POST",
  });
  return unwrapObject(result);
}

export async function addHomeUser(homeId, payload) {
  const result = await request(`/api/homes/${homeId}/users`, {
    method: "POST",
    body: payload,
  });
  return unwrapObject(result);
}

export async function updateHomeUser(homeId, userId, payload) {
  const result = await request(`/api/homes/${homeId}/users/${userId}`, {
    method: "PATCH",
    body: payload,
  });
  return unwrapObject(result);
}

export async function removeHomeUser(homeId, userId) {
  const result = await request(`/api/homes/${homeId}/users/${userId}`, {
    method: "DELETE",
  });
  return unwrapObject(result);
}

export async function setHomeUserPassword(homeId, userId, payload) {
  const result = await request(`/api/homes/${homeId}/users/${userId}/set-password`, {
    method: "POST",
    body: payload,
  });
  return unwrapObject(result);
}