import { request } from "./apiClient";

function unwrapApiData(response) {
  if (response && typeof response === "object" && "data" in response) {
    return response.data;
  }
  return response;
}

export async function fetchHistoryDevicesByHomeId(homeId) {
  const response = await request(`/api/devices/home/${homeId}`);
  const data = unwrapApiData(response);

  if (Array.isArray(data)) {
    return data;
  }

  if (Array.isArray(data?.devices)) {
    return data.devices;
  }

  return [];
}

export async function fetchHistoryActiveConfigByHomeId(homeId) {
  const response = await request(`/api/homes/${homeId}/configs/active`);
  return unwrapApiData(response) ?? null;
}

export async function fetchHistoryTelemetry(deviceKey, range = "24h") {
  const response = await request(`/api/v1/device/${deviceKey}/telemetry`, {
    query: { range },
  });

  return unwrapApiData(response) ?? {};
}