import { request, unwrapArray, unwrapObject } from "./apiClient";

export async function fetchDevicesByHomeId(homeId) {
  const result = await request(`/api/devices/home/${homeId}`);
  return unwrapArray(result);
}

export async function createDevice(homeId, payload, userId) {
  const result = await request(`/api/devices/home/${homeId}`, {
    method: "POST",
    query: { userId },
    body: payload,
  });
  return unwrapObject(result);
}