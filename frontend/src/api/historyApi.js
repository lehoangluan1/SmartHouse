import { request, unwrapArray, unwrapObject } from "./apiClient";

const DEVICES_BASE = "/api/devices";
const HOMES_BASE = "/api/homes";
const TELEMETRY_BASE = "/api/v1/device";

export async function fetchHistoryDevicesByHomeId(homeId) {
  const result = await request(`${DEVICES_BASE}/home/${homeId}`);
  const data = unwrapObject(result);

  if (data?.devices && Array.isArray(data.devices)) {
    return data.devices;
  }

  return unwrapArray(result);
}

export async function fetchHistoryActiveConfigByHomeId(homeId) {
  const result = await request(`${HOMES_BASE}/${homeId}/configs/active`);
  return unwrapObject(result);
}

export async function fetchHistoryTelemetry(deviceKey, range = "24h") {
  const result = await request(`${TELEMETRY_BASE}/${deviceKey}/telemetry`, {
    query: { range },
  });

  return unwrapObject(result) ?? {};
}