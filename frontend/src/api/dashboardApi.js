import { request, unwrapObject } from "./apiClient";

const DASHBOARD_BASE = "/api/dashboard";
const HOMES_BASE = "/api/homes";
const TELEMETRY_BASE = "/api/v1/device";
const CONTROL_BASE = "/api/control";

export async function fetchDashboardByHomeId(homeId) {
  const result = await request(`${DASHBOARD_BASE}/homes/${homeId}`);
  return unwrapObject(result);
}

export async function fetchActiveConfigByHomeId(homeId) {
  const result = await request(`${HOMES_BASE}/${homeId}/configs/active`);
  return unwrapObject(result);
}

export async function fetchDeviceTelemetry(deviceKey, range = "1h") {
  const result = await request(`${TELEMETRY_BASE}/${deviceKey}/telemetry`, {
    query: { range },
  });
  return unwrapObject(result);
}

export async function controlDevice(deviceId, payload) {
  const result = await request(`${CONTROL_BASE}/devices/${deviceId}`, {
    method: "POST",
    body: payload,
  });
  return unwrapObject(result);
}