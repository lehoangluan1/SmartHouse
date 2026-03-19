function ConfigActionBar({
  canSave = false,
  isDirty = false,
  canSetActive = false,
  canDelete = false,
  disabled = false,
  onSave,
  onCancel,
  onSetActive,
  onDelete,
}) {
  const saveDisabled = disabled || !canSave;

  return (
    <div className="config-actions">
      <button
        type="button"
        className="config-action-button config-action-button--success"
        disabled={disabled || !canSetActive}
        onClick={onSetActive}
      >
        {disabled ? "Processing..." : "Set Active"}
      </button>

      <button
        type="button"
        className="config-action-button config-action-button--primary"
        disabled={saveDisabled}
        onClick={onSave}
        title={!disabled && !isDirty ? "No changes to save" : ""}
      >
        {disabled ? "Saving..." : "Save Changes"}
      </button>

      <button
        type="button"
        className="config-action-button config-action-button--danger"
        disabled={disabled || !canDelete}
        onClick={onDelete}
      >
        {disabled ? "Processing..." : "Delete"}
      </button>

      <button
        type="button"
        className="config-action-button config-action-button--ghost"
        disabled={disabled}
        onClick={onCancel}
      >
        Cancel
      </button>
    </div>
  );
}

export default ConfigActionBar;