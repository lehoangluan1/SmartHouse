import { request, unwrapObject } from "./apiClient";

const AUDIT_BASE = "/api/audit";

export async function fetchAuditDashboard({
  homeId,
  from,
  to,
  configPage = 0,
  configSize = 10,
  configKeyword = "",
  eventPage = 0,
  eventSize = 20,
  eventKeyword = "",
  eventCategory = "all",
}) {
  const result = await request(`${AUDIT_BASE}/homes/${homeId}`, {
    query: {
      from,
      to,
      configPage,
      configSize,
      configKeyword,
      eventPage,
      eventSize,
      eventKeyword,
      eventCategory,
    },
  });

  return unwrapObject(result);
}