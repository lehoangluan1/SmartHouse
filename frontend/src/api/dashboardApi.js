import { request } from "./apiClient";

export async function fetchDashboardByHomeId(homeId) {
  const result = await request(`/api/dashboard/homes/${homeId}`);
  return result?.data ?? {};
}

export async function controlDevice(deviceId, payload) {
  const result = await request(`/api/control/devices/${deviceId}`, {
    method: "POST",
    body: payload,
  });

  return result?.data;
}

export async function fetchDeviceTelemetry(deviceKey, range = "1h") {
  const result = await request(`/api/v1/device/${deviceKey}/telemetry`, {
    query: { range },
  });

  return result?.data ?? {};
}

export async function fetchActiveConfigByHomeId(homeId) {
  const result = await request(`/api/homes/${homeId}/configs/active`);
  return result?.data ?? null;
}