import { useEffect, useMemo, useRef, useState } from "react";
import "./SettingsPage.css";
import SettingsSection from "../../components/settings/SettingsSection";
import ScheduleTable from "../../components/settings/ScheduleTable";
import PermissionTable from "../../components/settings/PermissionTable";
import ScheduleFormModal from "../../components/settings/ScheduleFormModal";
import ConfirmDialog from "../../components/common/ConfirmDialog";
import {
  fetchHomeModeSchedules,
  createHomeModeSchedule,
  updateHomeModeSchedule,
  deleteHomeModeSchedule,
} from "../../api/settingsApi";
import {
  fetchHomeUsers,
  updateHomeUser,
  removeHomeUser,
  setHomeUserPassword,
} from "../../api/homeUserApi";
import { useAuth } from "../../providers/AuthProvider";
import useConfirmDialog from "../../hooks/useConfirmDialog";

const MANAGE_HOME_USER_HOME_ROLES = new Set(["OWNER", "CO_OWNER"]);
const MANAGE_HOME_USER_SYSTEM_ROLES = new Set(["SUPER_ADMIN", "ADMIN"]);
const SCHEDULE_MANAGER_HOME_ROLES = new Set(["OWNER", "CO_OWNER"]);
const SCHEDULE_MANAGER_SYSTEM_ROLES = new Set(["SUPER_ADMIN", "ADMIN"]);

function SettingsPage() {
  const [schedules, setSchedules] = useState([]);
  const [users, setUsers] = useState([]);

  const [loading, setLoading] = useState(true);
  const [deletingScheduleId, setDeletingScheduleId] = useState(null);
  const [updatingUserId, setUpdatingUserId] = useState(null);
  const [removingUserId, setRemovingUserId] = useState(null);
  const [settingPasswordUserId, setSettingPasswordUserId] = useState(null);
  const [error, setError] = useState("");

  const [isScheduleModalOpen, setIsScheduleModalOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState(null);
  const [savingSchedule, setSavingSchedule] = useState(false);

  const [passwordDialog, setPasswordDialog] = useState({
    open: false,
    userId: null,
    username: "",
    value: "",
    error: "",
  });

  const actionLockRef = useRef(false);
  const passwordInputRef = useRef(null);

  const { user: currentUser } = useAuth();
  const homeId = currentUser?.homeId ? Number(currentUser.homeId) : null;

  const currentSystemRole = String(
    currentUser?.systemRole || currentUser?.role || ""
  ).toUpperCase();

  const currentHomeRole = String(
    currentUser?.homeRole || currentUser?.roleInHome || ""
  ).toUpperCase();

  const currentUserId = Number(currentUser?.userId ?? currentUser?.id ?? 0);

  const {
    confirm,
    dialogState,
    handleConfirm,
    handleCancel: handleDialogCancel,
  } = useConfirmDialog();

  const canManageUsers = useMemo(() => {
    return (
      MANAGE_HOME_USER_SYSTEM_ROLES.has(currentSystemRole) ||
      MANAGE_HOME_USER_HOME_ROLES.has(currentHomeRole)
    );
  }, [currentSystemRole, currentHomeRole]);

  const canManageSchedules = useMemo(() => {
    return (
      SCHEDULE_MANAGER_SYSTEM_ROLES.has(currentSystemRole) ||
      SCHEDULE_MANAGER_HOME_ROLES.has(currentHomeRole)
    );
  }, [currentSystemRole, currentHomeRole]);

  useEffect(() => {
    async function loadPageData() {
      if (!Number.isFinite(homeId) || homeId <= 0) {
        setError("Valid homeId not found");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError("");

        const [modeSchedulesRes, homeUsersRes] = await Promise.all([
          fetchHomeModeSchedules(homeId),
          fetchHomeUsers(homeId),
        ]);

        const modeSchedules = Array.isArray(modeSchedulesRes)
          ? modeSchedulesRes
          : Array.isArray(modeSchedulesRes?.items)
          ? modeSchedulesRes.items
          : Array.isArray(modeSchedulesRes?.data)
          ? modeSchedulesRes.data
          : Array.isArray(modeSchedulesRes?.result)
          ? modeSchedulesRes.result
          : [];

        const homeUsers = Array.isArray(homeUsersRes?.items)
          ? homeUsersRes.items
          : Array.isArray(homeUsersRes)
          ? homeUsersRes
          : Array.isArray(homeUsersRes?.data)
          ? homeUsersRes.data
          : [];

        setSchedules(mapSchedules(modeSchedules));
        setUsers(mapUsers(homeUsers));
      } catch (err) {
        setError(err.message || "Unable to load settings data");
      } finally {
        setLoading(false);
      }
    }

    loadPageData();
  }, [homeId, currentUserId]);

  useEffect(() => {
    if (!passwordDialog.open) return;
    const timer = setTimeout(() => {
      passwordInputRef.current?.focus();
    }, 0);
    return () => clearTimeout(timer);
  }, [passwordDialog.open]);

  function mapSchedules(list) {
    return (list || []).map((item) => {
      const startTime = formatTime(item.startTime);
      const endTime = formatTime(item.endTime);

      return {
        id: item.id,
        name: buildScheduleName(item),
        startTime,
        endTime,
        mode: String(item.mode || "").toUpperCase(),
        daysText: formatDaysMask(item.daysMask),
        enabled: Boolean(item.enabled),
        overnight: startTime > endTime,
        raw: item,
      };
    });
  }

  function mapUsers(list) {
    return (list || []).map((item) => {
      const id = Number(item.userId ?? item.id ?? 0);
      const allowProfileActivation = Boolean(item.allowProfileActivation);

      return {
        id,
        username: item.username || item.fullName || `user-${id}`,
        systemRole: String(item.systemRole || item.role || "").toUpperCase(),
        roleInHome: String(item.roleInHome || item.homeRole || "").toUpperCase(),
        provider: String(item.provider || item.authProvider || "").toUpperCase(),
        status: String(item.status || "").toUpperCase(),
        mustChangePassword: Boolean(item.mustChangePassword),
        allowProfileActivation,
        isPrimary: Boolean(item.isPrimary),
        isCurrentUser: id === currentUserId,
        activationLabel: allowProfileActivation
          ? "Allowed to enter home"
          : "Not allowed to enter home",
        raw: item,
      };
    });
  }

  function buildScheduleName(item) {
    if (item.name && String(item.name).trim()) return item.name;
    const mode = String(item.mode || "schedule").toLowerCase();
    return `${mode.charAt(0).toUpperCase()}${mode.slice(1)} Schedule`;
  }

  function formatTime(value) {
    if (!value) return "--:--";
    return String(value).slice(0, 5);
  }

  function formatDaysMask(daysMask) {
    if (daysMask == null) return "All Days";
    if (daysMask === 127) return "All Days";
    if (daysMask === 0) return "No Days";

    const labels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
    const result = [];

    for (let i = 0; i < 7; i += 1) {
      if ((daysMask & (1 << i)) !== 0) {
        result.push(labels[i]);
      }
    }

    return result.length ? result.join(", ") : "No Days";
  }

  function canEditTargetUser(target) {
    if (!target) return false;

    if (MANAGE_HOME_USER_SYSTEM_ROLES.has(currentSystemRole)) {
      return true;
    }

    if (currentHomeRole === "OWNER") {
      return target.roleInHome !== "OWNER" || target.isCurrentUser;
    }

    if (currentHomeRole === "CO_OWNER") {
      return !["OWNER", "CO_OWNER"].includes(target.roleInHome);
    }

    return false;
  }

  function canToggleProfileActivation(target) {
    if (!canManageUsers) return false;
    if (!target) return false;
    if (!canEditTargetUser(target)) return false;
    if (target.isPrimary) return false;
    return true;
  }

  function canRemoveTargetUser(target) {
    if (!canManageUsers) return false;
    if (!target) return false;
    if (target.isCurrentUser) return false;
    if (target.isPrimary) return false;
    return canEditTargetUser(target);
  }

  function canSetPasswordForTarget(target) {
    if (!canManageUsers) return false;
    if (!target) return false;
    if (target.provider !== "LOCAL") return false;
    return canEditTargetUser(target);
  }

  function openPasswordDialog(target) {
    setPasswordDialog({
      open: true,
      userId: target.id,
      username: target.username,
      value: "",
      error: "",
    });
  }

  function closePasswordDialog() {
    if (settingPasswordUserId != null || actionLockRef.current) return;

    setPasswordDialog({
      open: false,
      userId: null,
      username: "",
      value: "",
      error: "",
    });
  }

  function handlePasswordInputChange(event) {
    const nextValue = event.target.value;

    setPasswordDialog((prev) => ({
      ...prev,
      value: nextValue,
      error: prev.error ? "" : prev.error,
    }));
  }

  function handlePasswordDialogKeyDown(event) {
    if (event.key === "Escape") {
      event.preventDefault();
      closePasswordDialog();
      return;
    }

    if (event.key === "Enter") {
      event.preventDefault();
      handleConfirmSetPassword();
    }
  }

  function handleAddSchedule() {
    if (loading || savingSchedule || actionLockRef.current) return;
    if (!canManageSchedules) return;

    setEditingSchedule(null);
    setIsScheduleModalOpen(true);
  }

  function handleEditSchedule(schedule) {
    if (loading || savingSchedule || actionLockRef.current) return;
    if (!canManageSchedules) return;

    setEditingSchedule(schedule);
    setIsScheduleModalOpen(true);
  }

  function closeScheduleModal() {
    if (savingSchedule || actionLockRef.current) return;

    setIsScheduleModalOpen(false);
    setEditingSchedule(null);
  }

  async function handleSubmitSchedule(formValues) {
    if (actionLockRef.current || savingSchedule) return;

    if (!Number.isFinite(homeId) || homeId <= 0) {
      alert("Missing homeId");
      return;
    }

    if (!canManageSchedules) {
      alert("You do not have permission to manage schedules");
      return;
    }

    try {
      actionLockRef.current = true;
      setSavingSchedule(true);

      if (editingSchedule?.id) {
        const updated = await updateHomeModeSchedule(
          homeId,
          editingSchedule.id,
          formValues
        );

        const normalized = mapSchedules([updated])[0];

        setSchedules((prev) =>
          prev.map((item) => (item.id === editingSchedule.id ? normalized : item))
        );

        alert("Schedule updated successfully");
      } else {
        const created = await createHomeModeSchedule(homeId, formValues);
        const normalized = mapSchedules([created])[0];
        setSchedules((prev) => [normalized, ...prev]);

        alert("Schedule created successfully");
      }

      setIsScheduleModalOpen(false);
      setEditingSchedule(null);
    } catch (err) {
      alert(err.message || "Failed to save schedule");
    } finally {
      setSavingSchedule(false);
      actionLockRef.current = false;
    }
  }

  async function handleDeleteSchedule(scheduleId) {
    if (actionLockRef.current || deletingScheduleId != null) return;

    if (!Number.isFinite(homeId) || homeId <= 0) {
      alert("Missing homeId");
      return;
    }

    if (!canManageSchedules) {
      alert("You do not have permission to delete schedules");
      return;
    }

    const target = schedules.find((item) => item.id === scheduleId);

    const confirmed = await confirm({
      title: "Delete Schedule",
      message: `Are you sure you want to delete "${target?.name || "this schedule"}"?`,
      confirmText: "Delete",
      cancelText: "Cancel",
      tone: "danger",
    });

    if (!confirmed) return;

    try {
      actionLockRef.current = true;
      setDeletingScheduleId(scheduleId);

      await deleteHomeModeSchedule(homeId, scheduleId);
      setSchedules((prev) => prev.filter((item) => item.id !== scheduleId));
    } catch (err) {
      alert(err.message || "Failed to delete schedule");
    } finally {
      setDeletingScheduleId(null);
      actionLockRef.current = false;
    }
  }

  async function handleToggleProfileActivation(userId) {
    if (actionLockRef.current || updatingUserId != null) return;
    if (!Number.isFinite(homeId) || homeId <= 0 || !canManageUsers) return;

    const target = users.find((item) => item.id === userId);
    if (!target || !canToggleProfileActivation(target)) return;

    const nextValue = !target.allowProfileActivation;

    const confirmed = await confirm({
      title: nextValue ? "Allow Home Access" : "Block Home Access",
      message: nextValue
        ? `Allow "${target.username}" to enter this home?`
        : `Block "${target.username}" from entering this home?`,
      confirmText: nextValue ? "Allow" : "Block",
      cancelText: "Cancel",
      tone: nextValue ? "primary" : "danger",
    });

    if (!confirmed) return;

    try {
      actionLockRef.current = true;
      setUpdatingUserId(userId);

      const updated = await updateHomeUser(homeId, userId, {
        allowProfileActivation: nextValue,
      });

      const normalized = mapUsers([updated])[0];

      setUsers((prev) =>
        prev.map((user) => (user.id === userId ? normalized : user))
      );

      alert(
        nextValue
          ? `${target.username} has been allowed to enter home`
          : `${target.username} has been blocked from entering home`
      );
    } catch (err) {
      alert(err.message || "Failed to update home access");
    } finally {
      setUpdatingUserId(null);
      actionLockRef.current = false;
    }
  }

  function handleSetPassword(userId) {
    if (actionLockRef.current || settingPasswordUserId != null) return;
    if (!Number.isFinite(homeId) || homeId <= 0 || !canManageUsers) return;

    const target = users.find((item) => item.id === userId);
    if (!target || !canSetPasswordForTarget(target)) return;

    openPasswordDialog(target);
  }

  async function handleConfirmSetPassword() {
    if (actionLockRef.current || settingPasswordUserId != null) return;
    if (!Number.isFinite(homeId) || homeId <= 0 || !canManageUsers) return;

    const userId = Number(passwordDialog.userId);
    const trimmedPassword = String(passwordDialog.value || "").trim();

    if (!userId) return;

    if (!trimmedPassword) {
      setPasswordDialog((prev) => ({
        ...prev,
        error: "New password must not be blank",
      }));
      return;
    }

    try {
      actionLockRef.current = true;
      setSettingPasswordUserId(userId);

      await setHomeUserPassword(homeId, userId, {
        newPassword: trimmedPassword,
      });

      setPasswordDialog({
        open: false,
        userId: null,
        username: "",
        value: "",
        error: "",
      });

      alert("Password set successfully");
    } catch (err) {
      setPasswordDialog((prev) => ({
        ...prev,
        error: err.message || "Failed to set password",
      }));
    } finally {
      setSettingPasswordUserId(null);
      actionLockRef.current = false;
    }
  }

  async function handleRemoveUser(userId) {
    if (actionLockRef.current || removingUserId != null) return;
    if (!Number.isFinite(homeId) || homeId <= 0 || !canManageUsers) return;

    const target = users.find((item) => item.id === userId);
    if (!target || !canRemoveTargetUser(target)) return;

    const confirmed = await confirm({
      title: "Remove Member",
      message: `Are you sure you want to remove "${target.username}" from this household?`,
      confirmText: "Remove",
      cancelText: "Keep",
      tone: "danger",
    });

    if (!confirmed) return;

    try {
      actionLockRef.current = true;
      setRemovingUserId(userId);

      await removeHomeUser(homeId, userId);
      setUsers((prev) => prev.filter((item) => item.id !== userId));

      alert("Member removed from household");
    } catch (err) {
      alert(err.message || "Failed to remove member");
    } finally {
      setRemovingUserId(null);
      actionLockRef.current = false;
    }
  }

  return (
    <div className="settings-page">
      <div className="settings-page__content">
        {error ? <div className="settings-page__error">{error}</div> : null}

        <SettingsSection index="1." title="Automated Schedule">
          <div className="settings-page__actions">
            <button
              type="button"
              className="settings-primary-button"
              onClick={handleAddSchedule}
              disabled={loading || savingSchedule || !canManageSchedules}
            >
              <span className="settings-primary-button__icon">＋</span>
              <span>Add New Schedule</span>
            </button>
          </div>

          <ScheduleTable
            schedules={schedules}
            onEdit={canManageSchedules ? handleEditSchedule : undefined}
            onDelete={canManageSchedules ? handleDeleteSchedule : undefined}
            deletingId={deletingScheduleId}
          />
        </SettingsSection>

        <SettingsSection index="2." title="User Permissions">
          <PermissionTable
            users={users}
            disabled={!canManageUsers}
            updatingUserId={updatingUserId}
            removingUserId={removingUserId}
            settingPasswordUserId={settingPasswordUserId}
            onTogglePermission={handleToggleProfileActivation}
            onSetPassword={handleSetPassword}
            onRemoveUser={handleRemoveUser}
            canTogglePermission={canToggleProfileActivation}
            canSetPassword={canSetPasswordForTarget}
            canRemoveUser={canRemoveTargetUser}
          />
        </SettingsSection>
      </div>

      <ScheduleFormModal
        open={isScheduleModalOpen}
        saving={savingSchedule}
        initialData={editingSchedule?.raw || null}
        onClose={closeScheduleModal}
        onSubmit={handleSubmitSchedule}
      />

      <ConfirmDialog
        open={dialogState.open}
        title={dialogState.title}
        message={dialogState.message}
        confirmText={dialogState.confirmText}
        cancelText={dialogState.cancelText}
        tone={dialogState.tone}
        loading={
          savingSchedule ||
          deletingScheduleId != null ||
          updatingUserId != null ||
          removingUserId != null ||
          settingPasswordUserId != null
        }
        onConfirm={handleConfirm}
        onCancel={handleDialogCancel}
      />

      {passwordDialog.open ? (
        <div
          className="confirm-dialog__backdrop"
          onClick={closePasswordDialog}
          role="presentation"
        >
          <div
            className="confirm-dialog confirm-dialog--primary settings-password-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="settings-password-dialog-title"
            onClick={(event) => event.stopPropagation()}
            onKeyDown={handlePasswordDialogKeyDown}
          >
            <div className="confirm-dialog__header">
              <h3
                id="settings-password-dialog-title"
                className="confirm-dialog__title"
              >
                Set New Password
              </h3>
            </div>

            <div className="confirm-dialog__body">
              <p className="confirm-dialog__message">
                Enter a new password for{" "}
                <strong>{passwordDialog.username || "this user"}</strong>.
              </p>

              <div className="settings-password-dialog__field">
                <label
                  className="settings-password-dialog__label"
                  htmlFor="settings-password-input"
                >
                  New password
                </label>
                <input
                  ref={passwordInputRef}
                  id="settings-password-input"
                  type="password"
                  className="settings-password-dialog__input"
                  value={passwordDialog.value}
                  onChange={handlePasswordInputChange}
                  disabled={settingPasswordUserId != null}
                  autoComplete="new-password"
                  placeholder="Enter new password"
                />
                {passwordDialog.error ? (
                  <div className="settings-password-dialog__error">
                    {passwordDialog.error}
                  </div>
                ) : null}
              </div>
            </div>

            <div className="confirm-dialog__actions">
              <button
                type="button"
                className="confirm-dialog__button confirm-dialog__button--cancel"
                onClick={closePasswordDialog}
                disabled={settingPasswordUserId != null}
              >
                Cancel
              </button>
              <button
                type="button"
                className="confirm-dialog__button confirm-dialog__button--confirm"
                onClick={handleConfirmSetPassword}
                disabled={settingPasswordUserId != null}
              >
                {settingPasswordUserId != null ? "Saving..." : "Save Password"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

export default SettingsPage;