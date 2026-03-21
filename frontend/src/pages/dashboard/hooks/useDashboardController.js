import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  controlDevice,
  fetchActiveConfigByHomeId,
  fetchDashboardByHomeId,
} from "../../../api/dashboardApi";
import { DASHBOARD_POLL_MS } from "../../../utils/constants";
import { subscribeDashboardEvents } from "../../../api/dashboardRealtime";
import {
  buildConfiguredDashboardDevices,
  clampPercent,
  isControllerDevice,
  mapDeviceToCardModel,
  resolveIntensityTarget,
  resolveToggleTarget,
} from "../../../utils/deviceUtils";
import { buildMonitoringFromConfig } from "../../../utils/monitoringUtils";
import {
  applyOptimisticDeviceIntensity,
  applyOptimisticDeviceToggle,
  applyOptimisticModeChange,
  applyRealtimeEventToDashboard,
  applyRealtimeEventToMonitoring,
} from "../../../utils/dashboardStateUtils";
export function useDashboardController({ homeId, currentUser }) {
  const [dashboardData, setDashboardData] = useState(null);
  const [activeConfig, setActiveConfig] = useState(null);
  const [monitoring, setMonitoring] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [selectedDeviceId, setSelectedDeviceId] = useState(null);
  const [intensityDraftMap, setIntensityDraftMap] = useState({});

  const activeConfigRef = useRef(null);
  const rawDevicesRef = useRef([]);
  const pollingIntervalRef = useRef(null);
  const sseCleanupRef = useRef(null);
  const latestLoadIdRef = useRef(0);
  const mountedRef = useRef(false);

  const rawDevices = useMemo(
    () => (Array.isArray(dashboardData?.devices) ? dashboardData.devices : []),
    [dashboardData]
  );

  const monitoringSlots = activeConfig?.monitoringSlots || {};

  const devices = useMemo(() => {
    return buildConfiguredDashboardDevices(rawDevices, monitoringSlots).map(
      mapDeviceToCardModel
    );
  }, [rawDevices, monitoringSlots]);

  const selectedDevice = useMemo(() => {
    return devices.find((device) => device.id === selectedDeviceId) || null;
  }, [devices, selectedDeviceId]);

  const controllerDevice = useMemo(() => {
    return rawDevices.find(isControllerDevice) || null;
  }, [rawDevices]);

  const activeSegment = controllerDevice?.mode
    ? String(controllerDevice.mode).toUpperCase()
    : "AUTO";

  useEffect(() => {
    activeConfigRef.current = activeConfig;
  }, [activeConfig]);

  useEffect(() => {
    rawDevicesRef.current = rawDevices;
  }, [rawDevices]);

  const stopPolling = useCallback(() => {
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
      pollingIntervalRef.current = null;
    }
  }, []);

  const stopRealtime = useCallback(() => {
    if (typeof sseCleanupRef.current === "function") {
      sseCleanupRef.current();
      sseCleanupRef.current = null;
    }
  }, []);

  const ensureSelectedDevice = useCallback((configuredDevices) => {
    if (!Array.isArray(configuredDevices) || configuredDevices.length === 0) {
      setSelectedDeviceId(null);
      return;
    }

    setSelectedDeviceId((prev) => {
      if (prev && configuredDevices.some((d) => Number(d.id) === Number(prev))) {
        return prev;
      }
      return configuredDevices[0].id;
    });
  }, []);

  const loadDashboard = useCallback(
    async ({ silent = false } = {}) => {
      if (!homeId) {
        setDashboardData(null);
        setActiveConfig(null);
        setMonitoring([]);
        setSelectedDeviceId(null);
        setLoading(false);
        return;
      }

      const loadId = ++latestLoadIdRef.current;

      try {
        if (!silent) setLoading(true);
        setError("");

        const [data, fetchedActiveConfig] = await Promise.all([
          fetchDashboardByHomeId(homeId),
          fetchActiveConfigByHomeId(homeId),
        ]);

        if (!mountedRef.current || loadId !== latestLoadIdRef.current) return;

        const safeData = data || null;
        const safeConfig = fetchedActiveConfig || null;

        setDashboardData(safeData);
        setActiveConfig(safeConfig);

        const configuredControllableDevices = buildConfiguredDashboardDevices(
          safeData?.devices || [],
          safeConfig?.monitoringSlots || {}
        );

        ensureSelectedDevice(configuredControllableDevices);

        const monitoringItems = await buildMonitoringFromConfig(
          safeData?.devices || [],
          safeConfig
        );

        if (!mountedRef.current || loadId !== latestLoadIdRef.current) return;

        setMonitoring(monitoringItems);
      } catch (err) {
        if (!mountedRef.current || loadId !== latestLoadIdRef.current) return;

        setError(err?.message || "Failed to load dashboard");
        setDashboardData(null);
        setActiveConfig(null);
        setMonitoring([]);
        setSelectedDeviceId(null);
      } finally {
        if (mountedRef.current && loadId === latestLoadIdRef.current) {
          setLoading(false);
        }
      }
    },
    [ensureSelectedDevice, homeId]
  );

  const startPolling = useCallback(() => {
    if (!homeId || pollingIntervalRef.current) return;

    pollingIntervalRef.current = setInterval(() => {
      loadDashboard({ silent: true });
    }, DASHBOARD_POLL_MS);
  }, [homeId, loadDashboard]);

  const handleRealtimeEvent = useCallback((event) => {
    if (!event || typeof event !== "object") return;

    setDashboardData((prev) => applyRealtimeEventToDashboard(prev, event));

    setMonitoring((prev) =>
      applyRealtimeEventToMonitoring(
        prev,
        event,
        activeConfigRef.current,
        rawDevicesRef.current
      )
    );
  }, []);

  const startRealtime = useCallback(() => {
    if (!homeId) return;

    stopRealtime();

    const cleanup = subscribeDashboardEvents(homeId, {
      onOpen: () => {
        stopPolling();
      },
      onError: () => {
        startPolling();
      },
      onMessage: (event) => {
        handleRealtimeEvent(event);
      },
    });

    sseCleanupRef.current = cleanup;
  }, [handleRealtimeEvent, homeId, startPolling, stopPolling, stopRealtime]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!homeId) {
      stopRealtime();
      stopPolling();
      setDashboardData(null);
      setActiveConfig(null);
      setMonitoring([]);
      setSelectedDeviceId(null);
      setLoading(false);
      return;
    }

    loadDashboard();
    startRealtime();
    startPolling();

    return () => {
      stopRealtime();
      stopPolling();
    };
  }, [homeId, loadDashboard, startPolling, startRealtime, stopPolling, stopRealtime]);

  useEffect(() => {
    const nextMap = {};

    devices.forEach((item) => {
      nextMap[item.id] = item.intensity ?? 0;
    });

    setIntensityDraftMap((prev) => {
      const prevKeys = Object.keys(prev);
      const nextKeys = Object.keys(nextMap);

      if (
        prevKeys.length === nextKeys.length &&
        prevKeys.every((key) => prev[key] === nextMap[key])
      ) {
        return prev;
      }

      return nextMap;
    });
  }, [devices]);

  const handleToggleDevice = useCallback(
    async (device) => {
      const target = resolveToggleTarget(device);
      const nextValue = device.enabled ? "OFF" : "ON";

      if (!target) return;

      try {
        setActionLoading(true);
        setError("");

        setDashboardData((prev) =>
          applyOptimisticDeviceToggle(prev, device.id, nextValue)
        );

        await controlDevice(device.id, {
          target,
          value: nextValue,
          actorId: currentUser?.userId,
          actorName: currentUser?.username ?? "web-user",
          method: "app",
        });
      } catch (err) {
        setError(err?.message || "Failed to control device");
        await loadDashboard({ silent: true });
      } finally {
        setActionLoading(false);
      }
    },
    [currentUser, loadDashboard]
  );

  const handleIntensityChange = useCallback(
    async (device, intensity) => {
      const target = resolveIntensityTarget(device);
      if (!target) return;

      const safeValue = clampPercent(intensity);

      setIntensityDraftMap((prev) => ({
        ...prev,
        [device.id]: safeValue,
      }));

      try {
        setActionLoading(true);
        setError("");

        setDashboardData((prev) =>
          applyOptimisticDeviceIntensity(prev, device.id, safeValue)
        );

        await controlDevice(device.id, {
          target,
          value: String(safeValue),
          actorId: currentUser?.userId,
          actorName: currentUser?.username ?? "web-user",
          method: "app",
        });
      } catch (err) {
        setError(err?.message || "Failed to adjust device intensity");
        await loadDashboard({ silent: true });
      } finally {
        setActionLoading(false);
      }
    },
    [currentUser, loadDashboard]
  );

  const handleChangeMode = useCallback(
    async (mode) => {
      if (!controllerDevice) return;

      try {
        setActionLoading(true);
        setError("");

        setDashboardData((prev) =>
          applyOptimisticModeChange(prev, controllerDevice.id, mode)
        );

        await controlDevice(controllerDevice.id, {
          target: "mode",
          value: String(mode || "").toLowerCase(),
          actorId: currentUser?.userId,
          actorName: currentUser?.username ?? "web-user",
          method: "app",
        });
      } catch (err) {
        setError(err?.message || "Failed to change mode");
        await loadDashboard({ silent: true });
      } finally {
        setActionLoading(false);
      }
    },
    [controllerDevice, currentUser, loadDashboard]
  );

  return {
    loading,
    actionLoading,
    error,
    monitoring,
    devices,
    selectedDevice,
    selectedDeviceId,
    setSelectedDeviceId,
    intensityDraftMap,
    controllerDevice,
    activeSegment,
    handleToggleDevice,
    handleIntensityChange,
    handleChangeMode,
  };
}