import { useEffect, useMemo, useRef, useState } from "react";
import "./ConfigPage.css";
import {
  activateConfig,
  createConfig,
  deleteConfig,
  fetchConfigsByHomeId,
  updateConfig,
} from "../../api/configApi";
import {
  createDevice,
  fetchDevicesByHomeId,
} from "../../api/deviceApi";
import ConfigListPanel from "../../components/config/ConfigListPanel";
import ConfigDetailPanel, {
  TAB_THRESHOLDS,
} from "../../components/config/ConfigDetailPanel";
import CreateDeviceModal from "../../components/config/CreateDeviceModal";
import ConfirmDialog from "../../components/common/ConfirmDialog";
import { useAuth } from "../../providers/AuthProvider";
import useConfirmDialog from "../../hooks/useConfirmDialog";

export const DEFAULT_THRESHOLDS = {
  tHigh: 30,
  tLow: 27,
  lLow: 35,
  lHigh: 55,
  tSleepHigh: 29,
  tSleepLow: 26,
  tAwayHigh: 32,
  tCritical: 35,
  n: 2,
  m: 2,
  tHold: 5,
  dPresent: 3,
  k: 10,
  autoFanSpeed: 70,
  sleepFanSpeed: 40,
  awayFanSpeed: 55,
};

const DEFAULT_MONITORING_SLOTS = {
  temperatureDeviceId: null,
  humidityDeviceId: null,
  lightSensorDeviceId: null,
  motionDeviceId: null,
  fanDeviceId: null,
  lightDeviceId: null,
};

function createEmptyFormValues() {
  return {
    name: "",
    thresholds: { ...DEFAULT_THRESHOLDS },
    monitoringSlots: { ...DEFAULT_MONITORING_SLOTS },
  };
}

function normalizeConfigForm(values) {
  const thresholds = values?.thresholds || {};
  const monitoringSlots = values?.monitoringSlots || {};

  return {
    name: String(values?.name || "").trim(),
    thresholds: Object.keys(DEFAULT_THRESHOLDS).reduce((acc, key) => {
      const value = thresholds[key];
      acc[key] = value === "" || value == null ? null : Number(value);
      return acc;
    }, {}),
    monitoringSlots: Object.keys(DEFAULT_MONITORING_SLOTS).reduce((acc, key) => {
      const value = monitoringSlots[key];
      acc[key] = value == null || value === "" ? null : Number(value);
      return acc;
    }, {}),
  };
}

function toFormValuesFromConfig(config) {
  return {
    name: config?.name || "",
    thresholds: {
      ...DEFAULT_THRESHOLDS,
      ...(config?.thresholds || {}),
    },
    monitoringSlots: {
      ...DEFAULT_MONITORING_SLOTS,
      ...(config?.monitoringSlots || {}),
    },
  };
}

function ConfigPage() {
  const { user: currentUser } = useAuth();
  const homeId = currentUser?.homeId ? Number(currentUser.homeId) : null;
  const userId = currentUser?.userId;

  const normalizedRole = String(
    currentUser?.systemRole || currentUser?.role || ""
  ).toUpperCase();

  const isAdmin =
    currentUser?.isAdmin === true ||
    normalizedRole === "ADMIN" ||
    normalizedRole === "SUPER_ADMIN";

  const [configs, setConfigs] = useState([]);
  const [devices, setDevices] = useState([]);
  const [selectedConfigId, setSelectedConfigId] = useState(null);
  const [activeTab, setActiveTab] = useState(TAB_THRESHOLDS);
  const [deviceModalOpen, setDeviceModalOpen] = useState(false);
  const [deviceSaving, setDeviceSaving] = useState(false);
  const [deviceError, setDeviceError] = useState("");
  const [formValues, setFormValues] = useState(createEmptyFormValues());

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const saveLockRef = useRef(false);
  const deleteLockRef = useRef(false);
  const activateLockRef = useRef(false);
  const deviceCreateLockRef = useRef(false);

  const {
    confirm,
    dialogState,
    handleConfirm,
    handleCancel: handleDialogCancel,
  } = useConfirmDialog();

  useEffect(() => {
    loadPage();
  }, [homeId]);

  const selectedConfig = useMemo(
    () => configs.find((item) => item.id === selectedConfigId) || null,
    [configs, selectedConfigId]
  );

  const initialFormSnapshot = useMemo(() => {
    if (selectedConfig) {
      return normalizeConfigForm(toFormValuesFromConfig(selectedConfig));
    }
    return normalizeConfigForm(createEmptyFormValues());
  }, [selectedConfig]);

  const currentFormSnapshot = useMemo(
    () => normalizeConfigForm(formValues),
    [formValues]
  );

  const isDirty = useMemo(
    () =>
      JSON.stringify(currentFormSnapshot) !==
      JSON.stringify(initialFormSnapshot),
    [currentFormSnapshot, initialFormSnapshot]
  );

  const canSave = !loading && !saving && isDirty;

  async function loadPage() {
    if (!homeId) {
      setConfigs([]);
      setDevices([]);
      setSelectedConfigId(null);
      resetForm();
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError("");

      const [configList, deviceList] = await Promise.all([
        fetchConfigsByHomeId(homeId),
        fetchDevicesByHomeId(homeId),
      ]);

      setConfigs(configList || []);
      setDevices(deviceList || []);

      const initialConfig =
        (configList || []).find((item) => item.active) ||
        configList?.[0] ||
        null;

      if (initialConfig) {
        setSelectedConfigId(initialConfig.id);
        syncFormFromConfig(initialConfig);
      } else {
        setSelectedConfigId(null);
        resetForm();
      }
    } catch (err) {
      setError(err?.message || "Failed to load config");
    } finally {
      setLoading(false);
    }
  }

  function resolveMonitoringSlotBySubtype(subtype) {
    const normalized = String(subtype || "").toUpperCase();

    if (normalized === "TEMPERATURE_NODE") return "temperatureDeviceId";
    if (normalized === "HUMIDITY_NODE") return "humidityDeviceId";
    if (normalized === "LIGHT_NODE") return "lightSensorDeviceId";
    if (normalized === "MOTION_NODE") return "motionDeviceId";
    if (normalized === "FAN") return "fanDeviceId";
    if (normalized === "LIGHT") return "lightDeviceId";

    return null;
  }

  function syncFormFromConfig(config) {
    setFormValues(toFormValuesFromConfig(config));
  }

  function resetForm() {
    setFormValues(createEmptyFormValues());
  }

  function handleSelectConfig(config) {
    if (saving || loading) return;
    setSelectedConfigId(config.id);
    syncFormFromConfig(config);
    setActiveTab(TAB_THRESHOLDS);
    setError("");
  }

  function handleCreateNew() {
    if (saving || loading) return;
    setSelectedConfigId(null);
    resetForm();
    setActiveTab(TAB_THRESHOLDS);
    setError("");
  }

  function handleThresholdChange(field, value) {
    if (saving) return;

    setFormValues((prev) => ({
      ...prev,
      thresholds: {
        ...prev.thresholds,
        [field]: value,
      },
    }));
  }

  function handleSlotChange(field, value) {
    if (saving) return;

    setFormValues((prev) => ({
      ...prev,
      monitoringSlots: {
        ...prev.monitoringSlots,
        [field]: value ? Number(value) : null,
      },
    }));
  }

  function handleNameChange(value) {
    if (saving) return;

    setFormValues((prev) => ({
      ...prev,
      name: value,
    }));
  }

  function handleOpenCreateDeviceModal() {
    if (saving || deviceSaving) return;
    setDeviceError("");
    setDeviceModalOpen(true);
  }

  function handleCloseCreateDeviceModal() {
    if (deviceSaving) return;
    setDeviceError("");
    setDeviceModalOpen(false);
  }

  function handleCancel() {
    if (saving) return;

    setError("");

    if (selectedConfig) {
      syncFormFromConfig(selectedConfig);
    } else {
      resetForm();
    }

    setActiveTab(TAB_THRESHOLDS);
  }

  async function handleSave() {
    if (saveLockRef.current || saving || loading) return;
    if (!isDirty) return;
    if (!validateConfig(formValues, devices, setError)) return;

    saveLockRef.current = true;
    setSaving(true);
    setError("");

    try {
      const payload = {
        name: formValues.name.trim(),
        thresholds: normalizeConfigForm(formValues).thresholds,
        monitoringSlots: normalizeConfigForm(formValues).monitoringSlots,
      };

      const saved = selectedConfig?.id
        ? await updateConfig(homeId, selectedConfig.id, payload, userId)
        : await createConfig(homeId, payload, userId);

      await loadPage();

      if (saved?.id) {
        setSelectedConfigId(saved.id);
      }
    } catch (err) {
      setError(err?.message || "Failed to save config");
    } finally {
      setSaving(false);
      saveLockRef.current = false;
    }
  }

  async function handleDelete() {
    if (deleteLockRef.current || saving || loading) return;
    if (!selectedConfig?.id) return;

    const confirmed = await confirm({
      title: "Delete Config",
      message: `Are you sure you want to delete config "${selectedConfig.name}"? This action cannot be undone.`,
      confirmText: "Delete",
      cancelText: "Keep",
      tone: "danger",
    });

    if (!confirmed) return;

    deleteLockRef.current = true;
    setSaving(true);
    setError("");

    try {
      await deleteConfig(homeId, selectedConfig.id, userId);
      await loadPage();
    } catch (err) {
      setError(err?.message || "Failed to delete config");
    } finally {
      setSaving(false);
      deleteLockRef.current = false;
    }
  }

  async function handleCreateDevice(payload) {
    if (deviceCreateLockRef.current || deviceSaving || saving) return;

    deviceCreateLockRef.current = true;
    setDeviceSaving(true);
    setDeviceError("");

    try {
      const created = await createDevice(homeId, payload, userId);
      const deviceList = await fetchDevicesByHomeId(homeId);

      setDevices(deviceList || []);

      const slotField = resolveMonitoringSlotBySubtype(
        created?.subtype || created?.type
      );

      if (slotField && created?.id) {
        setFormValues((prev) => ({
          ...prev,
          monitoringSlots: {
            ...prev.monitoringSlots,
            [slotField]: created.id,
          },
        }));
      }

      setDeviceModalOpen(false);
    } catch (err) {
      setDeviceError(err?.message || "Failed to create device");
    } finally {
      setDeviceSaving(false);
      deviceCreateLockRef.current = false;
    }
  }

  async function handleSetActive() {
    if (activateLockRef.current || saving || loading) return;
    if (!selectedConfig?.id) return;
    if (selectedConfig?.active) return;

    activateLockRef.current = true;
    setSaving(true);
    setError("");

    try {
      await activateConfig(homeId, selectedConfig.id, userId);
      await loadPage();
    } catch (err) {
      setError(err?.message || "Failed to set active config");
    } finally {
      setSaving(false);
      activateLockRef.current = false;
    }
  }

  return (
    <div className="config-page">
      <div className="config-page__grid">
        <ConfigListPanel
          loading={loading}
          configs={configs}
          selectedConfigId={selectedConfigId}
          onSelectConfig={handleSelectConfig}
          onCreateNew={handleCreateNew}
        />

        <ConfigDetailPanel
          loading={loading}
          saving={saving}
          canSave={canSave}
          isDirty={isDirty}
          error={error}
          selectedConfig={selectedConfig}
          formValues={formValues}
          devices={devices}
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          isAdmin={isAdmin}
          onNameChange={handleNameChange}
          onThresholdChange={handleThresholdChange}
          onSlotChange={handleSlotChange}
          onSave={handleSave}
          onCancel={handleCancel}
          onSetActive={handleSetActive}
          onDelete={handleDelete}
          onCreateDevice={handleOpenCreateDeviceModal}
        />
      </div>

      <CreateDeviceModal
        open={deviceModalOpen}
        saving={deviceSaving}
        error={deviceError}
        onClose={handleCloseCreateDeviceModal}
        onSubmit={handleCreateDevice}
      />

      <ConfirmDialog
        open={dialogState.open}
        title={dialogState.title}
        message={dialogState.message}
        confirmText={dialogState.confirmText}
        cancelText={dialogState.cancelText}
        tone={dialogState.tone}
        loading={saving}
        onConfirm={handleConfirm}
        onCancel={handleDialogCancel}
      />
    </div>
  );
}

function getDeviceType(device) {
  return String(
    device?.subtype ||
      device?.type ||
      device?.deviceType ||
      ""
  ).toUpperCase();
}

function getDeviceClass(device) {
  return String(device?.deviceClass || device?.class || "").toUpperCase();
}

function findDevice(devices, deviceId) {
  return (devices || []).find((device) => Number(device.id) === Number(deviceId));
}

function validateSlotDevice(devices, deviceId, expectedClass, expectedType, label, setError) {
  if (deviceId == null) {
    return true;
  }

  const device = findDevice(devices, deviceId);
  if (!device) {
    setError(`${label} is not available in this home`);
    return false;
  }

  if (getDeviceClass(device) !== expectedClass || getDeviceType(device) !== expectedType) {
    setError(`${label} must use ${expectedType.replace("_", " ").toLowerCase()}`);
    return false;
  }

  return true;
}

function validateConfig(values, devices, setError) {
  if (!values?.name?.trim()) {
    setError("Config name must not be blank");
    return false;
  }

  const thresholds = values.thresholds || {};
  if (
    thresholds.tHigh != null &&
    thresholds.tLow != null &&
    Number(thresholds.tHigh) <= Number(thresholds.tLow)
  ) {
    setError("T_high must be greater than T_low");
    return false;
  }

  const slots = values.monitoringSlots || {};
  const ids = [
    slots.temperatureDeviceId,
    slots.humidityDeviceId,
    slots.lightSensorDeviceId,
    slots.motionDeviceId,
    slots.fanDeviceId,
    slots.lightDeviceId,
  ].filter((id) => id != null);

  if (new Set(ids).size !== ids.length) {
    setError("Monitoring slots must not have duplicate devices");
    return false;
  }

  if (
    !validateSlotDevice(
      devices,
      slots.lightSensorDeviceId,
      "SENSOR_NODE",
      "LIGHT_NODE",
      "Light sensor",
      setError
    )
  ) {
    return false;
  }

  if (
    !validateSlotDevice(
      devices,
      slots.fanDeviceId,
      "ACTUATOR",
      "FAN",
      "Fan device",
      setError
    )
  ) {
    return false;
  }

  if (
    !validateSlotDevice(
      devices,
      slots.lightDeviceId,
      "ACTUATOR",
      "LIGHT",
      "Light device",
      setError
    )
  ) {
    return false;
  }

  setError("");
  return true;
}

export default ConfigPage;
