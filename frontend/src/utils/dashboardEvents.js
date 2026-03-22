import { API_BASE_URL } from "./apiClient";
import { dispatchLogoutEvent } from "../utils/authEvents";

const DEFAULT_HEARTBEAT_TIMEOUT_MS = 45_000;

function parseEventData(event) {
  if (!event?.data) return null;

  try {
    return JSON.parse(event.data);
  } catch (error) {
    console.error("Invalid SSE payload", error, event.data);
    return null;
  }
}

function createConnectionStateController(callbacks = {}) {
  let currentState = "connecting";

  const setState = (nextState, meta = {}) => {
    if (currentState === nextState) return;
    currentState = nextState;
    callbacks.onStateChange?.(nextState, meta);
  };

  const getState = () => currentState;

  return { setState, getState };
}

export function subscribeDashboardEvents(
  homeId,
  {
    onMessage,
    onOpen,
    onError,
    onStateChange,
    onHeartbeatTimeout,
  } = {}
) {
  if (!homeId) {
    onStateChange?.("disconnected", { reason: "missing-home-id" });
    return () => {};
  }

  const url = `${API_BASE_URL}/api/dashboard/homes/${homeId}/stream`;
  const eventSource = new EventSource(url, { withCredentials: true });

  const state = createConnectionStateController({ onStateChange });

  let manuallyClosed = false;
  let heartbeatTimer = null;
  let hasEverConnected = false;

  const clearHeartbeatTimer = () => {
    if (heartbeatTimer) {
      clearTimeout(heartbeatTimer);
      heartbeatTimer = null;
    }
  };

  const resetHeartbeatTimer = () => {
    clearHeartbeatTimer();
    heartbeatTimer = setTimeout(() => {
      if (manuallyClosed) return;

      state.setState("reconnecting", { reason: "heartbeat-timeout" });
      onHeartbeatTimeout?.();

      try {
        eventSource.close();
      } catch {
        // ignore
      }
    }, DEFAULT_HEARTBEAT_TIMEOUT_MS);
  };

  const forwardEvent = (event) => {
    const payload = parseEventData(event);
    if (!payload) return;

    if (event.type === "HEARTBEAT" || event.type === "CONNECTED") {
      resetHeartbeatTimer();
    }

    onMessage?.(payload, event.type);
  };

  eventSource.onopen = () => {
    hasEverConnected = true;
    state.setState("connected", { source: "onopen" });
    resetHeartbeatTimer();
    onOpen?.();
  };

  eventSource.onerror = (error) => {
    clearHeartbeatTimer();

    if (manuallyClosed) {
      state.setState("disconnected", { reason: "manual-close" });
      return;
    }

    const readyState = eventSource.readyState;

    if (readyState === EventSource.CONNECTING) {
      state.setState("reconnecting", { reason: "network-or-server" });
    } else if (readyState === EventSource.CLOSED) {
      state.setState("disconnected", {
        reason: hasEverConnected ? "closed-after-connect" : "failed-initial-connect",
      });
    } else {
      state.setState("reconnecting", { reason: "unknown-error" });
    }

    onError?.(error, {
      readyState,
      hasEverConnected,
    });
  };

  eventSource.addEventListener("CONNECTED", forwardEvent);
  eventSource.addEventListener("DEVICE_STATE_CHANGED", forwardEvent);
  eventSource.addEventListener("HOME_MODE_CHANGED", forwardEvent);
  eventSource.addEventListener("TELEMETRY_RECEIVED", forwardEvent);
  eventSource.addEventListener("HEARTBEAT", forwardEvent);

  const handleForceLogout = () => {
    manuallyClosed = true;
    clearHeartbeatTimer();
    try {
      eventSource.close();
    } catch {
      // ignore
    }
    state.setState("disconnected", { reason: "logout" });
  };

  window.addEventListener("app:logout", handleForceLogout);

  return () => {
    manuallyClosed = true;
    clearHeartbeatTimer();
    window.removeEventListener("app:logout", handleForceLogout);

    try {
      eventSource.close();
    } catch {
      // ignore
    }

    state.setState("disconnected", { reason: "cleanup" });
  };
}

export function forceDashboardSseDisconnectOnLogout() {
  dispatchLogoutEvent();
}