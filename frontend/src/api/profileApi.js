import { request, unwrapObject } from "./apiClient";

const PROFILE_BASE = "/api/profile";

export async function getMyProfile() {
  const result = await request(`${PROFILE_BASE}/me`);
  return unwrapObject(result);
}

export async function changeMyPassword(payload) {
  const result = await request(`${PROFILE_BASE}/password`, {
    method: "PATCH",
    body: payload,
  });
  return unwrapObject(result);
}