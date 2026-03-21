import "./DashboardPage.css";
import MonitoringCard from "../../components/dashboard/MonitoringCard";
import DeviceSwitchCard from "../../components/dashboard/DeviceSwitchCard";
import SegmentControl from "../../components/dashboard/SegmentControl";
import { SEGMENTS } from "../../utils/constants";
import { useDashboardController } from "./hooks/useDashboardController";
import { useAuth } from "../../providers/AuthProvider";

function DashboardPage() {
  const { user: currentUser } = useAuth();
  const homeId = currentUser?.homeId ? Number(currentUser.homeId) : null;

  const {
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
  } = useDashboardController({
    homeId,
    currentUser,
  });

  const resolveDisplayIntensity = (device) => {
    const draftValue = intensityDraftMap[device.id];
    const serverValue = Number(device.intensity ?? 0);
    const normalizedServerValue = Number.isFinite(serverValue)
      ? Math.max(0, Math.min(100, serverValue))
      : 0;

    if (!device.enabled) {
      return 0;
    }

    if (draftValue !== undefined && draftValue !== null) {
      const normalizedDraftValue = Number(draftValue);
      return Number.isFinite(normalizedDraftValue)
        ? Math.max(0, Math.min(100, normalizedDraftValue))
        : normalizedServerValue;
    }

    return normalizedServerValue;
  };

  return (
    <div className="dashboard-page">
      <div className="dashboard-content">
        {loading ? (
          <div className="dashboard-panel">
            <div className="dashboard-panel__body">Loading...</div>
          </div>
        ) : (
          <>
            <section className="dashboard-panel">
              <div className="dashboard-panel__header">
                <h2>Monitoring</h2>
              </div>

              <div className="dashboard-panel__body dashboard-monitoring-list">
                {monitoring.map((item) => (
                  <MonitoringCard key={item.id} item={item} />
                ))}
              </div>
            </section>

            <section className="dashboard-panel">
              <div className="dashboard-panel__header">
                <h2>Control/Switch</h2>
              </div>

              <div className="dashboard-panel__body dashboard-device-list">
                {devices.length > 0 ? (
                  devices.map((device) => {
                    const displayIntensity = resolveDisplayIntensity(device);

                    return (
                      <DeviceSwitchCard
                        key={device.id}
                        device={{
                          ...device,
                          intensity: displayIntensity,
                        }}
                        selected={selectedDeviceId === device.id}
                        disabled={actionLoading}
                        onSelect={() => setSelectedDeviceId(device.id)}
                        onToggle={() => handleToggleDevice(device)}
                        onIntensityChange={(value) =>
                          handleIntensityChange(device, value)
                        }
                      />
                    );
                  })
                ) : (
                  <div className="dashboard-panel__body">
                    No active configured controllable devices
                  </div>
                )}
              </div>

              {controllerDevice ? (
                <SegmentControl
                  title={`System Mode${
                    controllerDevice?.name ? ` (${controllerDevice.name})` : ""
                  }`}
                  options={SEGMENTS}
                  activeValue={activeSegment}
                  disabled={actionLoading}
                  onChange={handleChangeMode}
                />
              ) : (
                <div className="dashboard-mode-disabled">
                  No controller found that supports mode
                </div>
              )}

              <div className="dashboard-integrations">
                <div className="dashboard-integration-item">
                  <span className="dashboard-status-dot online" />
                  <span>OhStem</span>
                </div>
                <div className="dashboard-integration-item">
                  <span className="dashboard-status-dot online" />
                  <span>Device Control</span>
                </div>
              </div>

              {selectedDevice ? (
                <div className="dashboard-selected-device-note">
                  Selected: <strong>{selectedDevice.name}</strong>
                </div>
              ) : null}

              {error ? (
                <div className="dashboard-error-text" role="alert">
                  {error}
                </div>
              ) : null}
            </section>
          </>
        )}
      </div>
    </div>
  );
}

export default DashboardPage;