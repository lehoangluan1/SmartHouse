import { request } from "./apiClient";

export async function fetchHomeModeSchedules(homeId) {
  const res = await request(`/api/homes/${homeId}/mode-schedules`);
  return res?.data || [];
}

export async function createHomeModeSchedule(homeId, payload) {
  const res = await request(`/api/homes/${homeId}/mode-schedules`, {
    method: "POST",
    body: payload,
  });
  return res?.data;
}

export async function updateHomeModeSchedule(homeId, scheduleId, payload) {
  const res = await request(`/api/homes/${homeId}/mode-schedules/${scheduleId}`, {
    method: "PUT",
    body: payload,
  });
  return res?.data;
}

export async function deleteHomeModeSchedule(homeId, scheduleId) {
  const res = await request(`/api/homes/${homeId}/mode-schedules/${scheduleId}`, {
    method: "DELETE",
  });
  return res?.data;
}