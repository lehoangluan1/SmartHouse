import { API_BASE_URL } from "./apiClient";

function parseEventData(event) {
  if (!event?.data) return null;

  try {
    return JSON.parse(event.data);
  } catch (error) {
    console.error("Invalid SSE payload", error, event.data);
    return null;
  }
}

export function subscribeDashboardEvents(
  homeId,
  { onMessage, onOpen, onError, onStateChange } = {}
) {
  if (!homeId) return () => {};

  const url = `${API_BASE_URL}/api/dashboard/homes/${homeId}/stream`;
  const eventSource = new EventSource(url, { withCredentials: true });

  const forwardEvent = (event) => {
    const payload = parseEventData(event);
    if (!payload) return;
    onMessage?.(payload, event.type);
  };

  eventSource.onopen = () => {
    onStateChange?.("connected");
    onOpen?.();
  };

  eventSource.onerror = (error) => {
    onStateChange?.("reconnecting");
    onError?.(error);
  };

  eventSource.addEventListener("CONNECTED", forwardEvent);
  eventSource.addEventListener("DEVICE_STATE_CHANGED", forwardEvent);
  eventSource.addEventListener("HOME_MODE_CHANGED", forwardEvent);
  eventSource.addEventListener("TELEMETRY_RECEIVED", forwardEvent);
  eventSource.addEventListener("HEARTBEAT", forwardEvent);

  return () => {
    onStateChange?.("disconnected");
    eventSource.close();
  };
}