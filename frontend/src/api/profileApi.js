import { request, unwrapObject } from "./apiClient";

export async function getMyProfile() {
  const result = await request("/api/profile/me");
  return unwrapObject(result);
}

export async function changeMyPassword(payload) {
  const result = await request("/api/profile/password", {
    method: "PATCH",
    body: payload,
  });
  return unwrapObject(result);
}