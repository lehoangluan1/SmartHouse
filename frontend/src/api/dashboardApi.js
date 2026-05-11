import { request, unwrapObject } from "./apiClient";

const DASHBOARD_BASE = "/api/dashboard";
const HOMES_BASE = "/api/homes";
const TELEMETRY_BASE = "/api/v1/device";
const CONTROL_BASE = "/api/control";
const GATEWAY_BASE_URL =
  import.meta.env.VITE_GATEWAY_BASE_URL?.replace(/\/+$/, "") ||
  "http://localhost:9000";

export async function fetchDashboardByHomeId(homeId) {
  const result = await request(`${DASHBOARD_BASE}/homes/${homeId}`);
  return unwrapObject(result);
}

export async function fetchActiveConfigByHomeId(homeId) {
  const result = await request(`${HOMES_BASE}/${homeId}/configs/active`);
  return unwrapObject(result);
}

export async function fetchDeviceTelemetry(deviceKey, range = "1h") {
  try {
    const result = await request(`${TELEMETRY_BASE}/${deviceKey}/telemetry`, {
      query: { range },
    });
    const telemetry = unwrapObject(result);
    if (Array.isArray(telemetry?.items) && telemetry.items.length > 0) {
      return telemetry;
    }
  } catch {
    // Gateway cache is the safety net when the cloud/backend is slow or unavailable.
  }

  return fetchGatewayTelemetrySnapshot(deviceKey, range);
}

export async function controlDevice(deviceId, payload) {
  const result = await request(`${CONTROL_BASE}/devices/${deviceId}`, {
    method: "POST",
    body: payload,
  });
  return unwrapObject(result);
}

async function fetchGatewayTelemetrySnapshot(deviceKey, range) {
  const url = new URL(
    `${GATEWAY_BASE_URL}/gw/v1/device/${encodeURIComponent(deviceKey)}/telemetry`
  );
  url.searchParams.set("range", range);

  const response = await fetch(url.toString(), {
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    return null;
  }

  const json = await response.json().catch(() => null);
  return unwrapObject(json);
}
