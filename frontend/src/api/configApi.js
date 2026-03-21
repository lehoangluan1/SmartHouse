import { request, unwrapArray, unwrapObject } from "./apiClient";

const HOMES_BASE = "/api/homes";

export async function fetchConfigsByHomeId(homeId) {
  const result = await request(`${HOMES_BASE}/${homeId}/configs`);
  return unwrapArray(result);
}

export async function createConfig(homeId, payload, userId) {
  const result = await request(`${HOMES_BASE}/${homeId}/configs`, {
    method: "POST",
    query: { userId },
    body: payload,
  });
  return unwrapObject(result);
}

export async function updateConfig(homeId, configId, payload, userId) {
  const result = await request(`${HOMES_BASE}/${homeId}/configs/${configId}`, {
    method: "PUT",
    query: { userId },
    body: payload,
  });
  return unwrapObject(result);
}

export async function activateConfig(homeId, configId, userId) {
  const result = await request(
    `${HOMES_BASE}/${homeId}/configs/${configId}/activate`,
    {
      method: "PUT",
      query: { userId },
    }
  );
  return unwrapObject(result);
}

export async function deleteConfig(homeId, configId, userId) {
  const result = await request(`${HOMES_BASE}/${homeId}/configs/${configId}`, {
    method: "DELETE",
    query: { userId },
  });
  return unwrapObject(result);
}