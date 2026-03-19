import ThresholdForm from "./ThresholdForm";
import MonitoringDeviceSelector from "./MonitoringDeviceSelector";
import RuleSummaryCard from "./RuleSummaryCard";
import ConfigActionBar from "./ConfigActionBar";

export const TAB_THRESHOLDS = "thresholds";
export const TAB_DEVICES = "devices";

function ConfigDetailPanel({
  loading,
  saving,
  canSave,
  isDirty,
  error,
  selectedConfig,
  formValues,
  devices,
  activeTab,
  setActiveTab,
  isAdmin,
  onNameChange,
  onThresholdChange,
  onSlotChange,
  onSave,
  onCancel,
  onSetActive,
  onDelete,
  onCreateDevice,
}) {
  if (loading) {
    return (
      <section className="config-panel config-detail-panel">
        <div className="config-panel__header">
          <h2>Config Detail</h2>
        </div>
        <div className="config-loading">Loading...</div>
      </section>
    );
  }

  const selectedDeviceCount = Object.values(
    formValues?.monitoringSlots || {}
  ).filter(Boolean).length;

  return (
    <section className="config-panel config-detail-panel">
      <div className="config-panel__header config-detail-panel__header">
        <div>
          <h2>
            {formValues?.name || selectedConfig?.name || "New Config"}
            {selectedConfig?.ownerName || selectedConfig?.owner ? (
              <span className="config-detail-panel__owner">
                ({selectedConfig.ownerName || selectedConfig.owner})
              </span>
            ) : null}
          </h2>
          <p>
            {selectedConfig?.active
              ? "Currently used as monitoring configuration"
              : "Configure rules and select sensors to display in monitoring"}
          </p>
        </div>
      </div>

      <div className="config-name-box">
        <label className="config-name-field">
          <span className="config-name-field__label">Config name</span>
          <input
            type="text"
            className="config-name-field__input"
            value={formValues?.name || ""}
            disabled={saving}
            onChange={(event) => onNameChange(event.target.value)}
            placeholder="Enter config name"
          />
        </label>
      </div>

      <div className="config-tabs">
        <button
          type="button"
          className={`config-tab ${
            activeTab === TAB_THRESHOLDS ? "is-active" : ""
          }`}
          onClick={() => setActiveTab(TAB_THRESHOLDS)}
          disabled={saving}
        >
          Thresholds
        </button>

        <button
          type="button"
          className={`config-tab ${
            activeTab === TAB_DEVICES ? "is-active" : ""
          }`}
          onClick={() => setActiveTab(TAB_DEVICES)}
          disabled={saving}
        >
          Device Selector
          <span className="config-tab__badge">{selectedDeviceCount}</span>
        </button>
      </div>

      {activeTab === TAB_THRESHOLDS ? (
        <div className="config-thresholds">
          <ThresholdForm
            values={formValues?.thresholds}
            disabled={saving}
            onChange={onThresholdChange}
          />
          <RuleSummaryCard thresholds={formValues?.thresholds} />
        </div>
      ) : (
        <div className="config-devices">
          <MonitoringDeviceSelector
            devices={devices}
            value={formValues?.monitoringSlots}
            disabled={saving}
            isAdmin={isAdmin}
            onChange={onSlotChange}
            onCreateDevice={onCreateDevice}
          />
        </div>
      )}

      <ConfigActionBar
        canSave={canSave}
        isDirty={isDirty}
        canSetActive={!!selectedConfig?.id && !selectedConfig?.active}
        canDelete={!!selectedConfig?.id}
        disabled={saving}
        onSave={onSave}
        onCancel={onCancel}
        onSetActive={onSetActive}
        onDelete={onDelete}
      />

      {error ? <div className="config-error-text">{error}</div> : null}
    </section>
  );
}

export default ConfigDetailPanel;