import { request, unwrapArray, unwrapObject } from "./apiClient";

export async function fetchConfigsByHomeId(homeId) {
  const result = await request(`/api/homes/${homeId}/configs`);
  return unwrapArray(result);
}

export async function createConfig(homeId, payload, userId) {
  const result = await request(`/api/homes/${homeId}/configs`, {
    method: "POST",
    query: { userId },
    body: payload,
  });
  return unwrapObject(result);
}

export async function updateConfig(homeId, configId, payload, userId) {
  const result = await request(`/api/homes/${homeId}/configs/${configId}`, {
    method: "PUT",
    query: { userId },
    body: payload,
  });
  return unwrapObject(result);
}

export async function activateConfig(homeId, configId, userId) {
  const result = await request(`/api/homes/${homeId}/configs/${configId}/activate`, {
    method: "POST",
    query: { userId },
  });
  return unwrapObject(result);
}

export async function deleteConfig(homeId, configId, userId) {
  const result = await request(`/api/homes/${homeId}/configs/${configId}`, {
    method: "DELETE",
    query: { userId },
  });
  return unwrapObject(result);
}