import { request, unwrapArray, unwrapObject } from "./apiClient";

const HOMES_BASE = "/api/homes";

export async function fetchHomeModeSchedules(homeId) {
  const result = await request(`${HOMES_BASE}/${homeId}/mode-schedules`);
  return unwrapArray(result);
}

export async function createHomeModeSchedule(homeId, payload) {
  const result = await request(`${HOMES_BASE}/${homeId}/mode-schedules`, {
    method: "POST",
    body: payload,
  });
  return unwrapObject(result);
}

export async function updateHomeModeSchedule(homeId, scheduleId, payload) {
  const result = await request(
    `${HOMES_BASE}/${homeId}/mode-schedules/${scheduleId}`,
    {
      method: "PUT",
      body: payload,
    }
  );
  return unwrapObject(result);
}

export async function deleteHomeModeSchedule(homeId, scheduleId) {
  const result = await request(
    `${HOMES_BASE}/${homeId}/mode-schedules/${scheduleId}`,
    {
      method: "DELETE",
    }
  );
  return unwrapObject(result);
}