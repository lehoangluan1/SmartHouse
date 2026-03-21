import { request, unwrapArray, unwrapObject } from "./apiClient";

const DEVICES_BASE = "/api/devices";

export async function fetchDevicesByHomeId(homeId) {
  const result = await request(`${DEVICES_BASE}/home/${homeId}`);
  return unwrapArray(result);
}

export async function createDevice(homeId, payload, userId) {
  const result = await request(`${DEVICES_BASE}/home/${homeId}`, {
    method: "POST",
    query: { userId },
    body: payload,
  });
  return unwrapObject(result);
}