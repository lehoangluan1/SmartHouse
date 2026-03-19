import { request } from "./apiClient";

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
  const res = await request(`/api/audit/homes/${homeId}`, {
    method: "GET",
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

  return res?.data ?? res;
}