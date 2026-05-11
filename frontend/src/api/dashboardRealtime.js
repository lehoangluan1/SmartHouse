import { API_BASE_URL } from "./apiClient";
import { dispatchLogoutEvent } from "../utils/authEvents";

const DEFAULT_HEARTBEAT_TIMEOUT_MS = 45_000;
const RECONNECT_DELAY_MS = 1_000;
const isDev = import.meta.env.DEV;

function devLog(...args) {
  if (isDev) {
    console.debug("[dashboard-sse]", ...args);
  }
}

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

  return { setState };
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
  const state = createConnectionStateController({ onStateChange });

  let eventSource = null;
  let manuallyClosed = false;
  let heartbeatTimer = null;
  let reconnectTimer = null;
  let hasEverConnected = false;

  const clearHeartbeatTimer = () => {
    if (heartbeatTimer) {
      clearTimeout(heartbeatTimer);
      heartbeatTimer = null;
    }
  };

  const clearReconnectTimer = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
  };

  const closeCurrentSource = () => {
    if (!eventSource) return;

    try {
      eventSource.close();
    } catch {
      // ignore
    }
    eventSource = null;
  };

  const scheduleReconnect = (reason) => {
    if (manuallyClosed || reconnectTimer) return;

    devLog("reconnect scheduled", { homeId, reason });
    state.setState(hasEverConnected ? "reconnecting" : "connecting", { reason });
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      connect();
    }, RECONNECT_DELAY_MS);
  };

  const resetHeartbeatTimer = () => {
    clearHeartbeatTimer();
    heartbeatTimer = setTimeout(() => {
      if (manuallyClosed) return;

      devLog("heartbeat timeout", { homeId });
      state.setState("reconnecting", { reason: "heartbeat-timeout" });
      onHeartbeatTimeout?.();
      closeCurrentSource();
      scheduleReconnect("heartbeat-timeout");
    }, DEFAULT_HEARTBEAT_TIMEOUT_MS);
  };

  const forwardEvent = (event) => {
    const payload = parseEventData(event);
    if (!payload) return;

    resetHeartbeatTimer();
    devLog("event received", event.type, payload);
    onMessage?.(payload, event.type);
  };

  function connect() {
    if (manuallyClosed) return;

    closeCurrentSource();
    clearHeartbeatTimer();

    devLog("connecting", { homeId, url });
    eventSource = new EventSource(url, { withCredentials: true });

    eventSource.onopen = () => {
      hasEverConnected = true;
      devLog("connected", { homeId });
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

      const readyState = eventSource?.readyState;
      devLog("error", { homeId, readyState, hasEverConnected });

      if (readyState === EventSource.CONNECTING) {
        state.setState("reconnecting", { reason: "network-or-server" });
      } else if (readyState === EventSource.CLOSED) {
        state.setState("disconnected", {
          reason: hasEverConnected ? "closed-after-connect" : "failed-initial-connect",
        });
        scheduleReconnect("closed");
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
  }

  const handleForceLogout = () => {
    manuallyClosed = true;
    clearHeartbeatTimer();
    clearReconnectTimer();
    closeCurrentSource();
    state.setState("disconnected", { reason: "logout" });
  };

  window.addEventListener("app:logout", handleForceLogout);
  connect();

  return () => {
    manuallyClosed = true;
    clearHeartbeatTimer();
    clearReconnectTimer();
    window.removeEventListener("app:logout", handleForceLogout);
    closeCurrentSource();
    state.setState("disconnected", { reason: "cleanup" });
  };
}

export function forceDashboardSseDisconnectOnLogout() {
  dispatchLogoutEvent();
}
