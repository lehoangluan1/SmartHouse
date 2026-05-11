import { useEffect, useMemo, useState } from "react";
import { subscribeDashboardEvents } from "../../../api/dashboardRealtime";

export function useDashboardSse(homeId, { onEvent } = {}) {
  const [connectionState, setConnectionState] = useState(
    homeId ? "connecting" : "disconnected"
  );
  const [lastEventType, setLastEventType] = useState(null);
  const [lastEventAt, setLastEventAt] = useState(null);
  const [lastError, setLastError] = useState(null);

  useEffect(() => {
    if (!homeId) {
      setConnectionState("disconnected");
      setLastEventType(null);
      setLastEventAt(null);
      setLastError(null);
      return;
    }

    setConnectionState("connecting");
    setLastError(null);

    const unsubscribe = subscribeDashboardEvents(homeId, {
      onOpen: () => {
        setConnectionState("connected");
      },
      onError: (error, meta) => {
        setLastError({ error, meta, at: Date.now() });
      },
      onStateChange: (state) => {
        setConnectionState(state);
      },
      onMessage: (payload, eventType) => {
        setLastEventType(eventType);
        setLastEventAt(Date.now());
        onEvent?.(payload, eventType);
      },
    });

    return () => {
      unsubscribe();
    };
  }, [homeId, onEvent]);

  return useMemo(
    () => ({
      connectionState,
      isConnected: connectionState === "connected",
      isReconnecting: connectionState === "reconnecting",
      isDisconnected: connectionState === "disconnected",
      isConnecting: connectionState === "connecting",
      lastEventType,
      lastEventAt,
      lastError,
    }),
    [connectionState, lastEventType, lastEventAt, lastError]
  );
}
