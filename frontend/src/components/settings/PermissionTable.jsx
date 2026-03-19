import "./PermissionTable.css";

function getUserInitials(name) {
  const text = String(name || "").trim();
  if (!text) return "?";

  const parts = text.split(/\s+/).filter(Boolean);
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }

  return `${parts[0][0] || ""}${parts[parts.length - 1][0] || ""}`.toUpperCase();
}

function PermissionTable({
  users,
  disabled,
  updatingUserId,
  removingUserId,
  settingPasswordUserId,
  onTogglePermission,
  onSetPassword,
  onRemoveUser,
  canTogglePermission,
  canSetPassword,
  canRemoveUser,
  error,
}) {
  return (
    <div className="settings-table-card">
      {error ? <div className="permission-table__error">{error}</div> : null}

      <table className="settings-table">
        <thead>
          <tr>
            <th>User</th>
            <th>System Role</th>
            <th>Home Role</th>
            <th>Provider</th>
            <th>Profile Access</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          {users.length === 0 ? (
            <tr>
              <td colSpan="6" className="settings-table__empty">
                No members
              </td>
            </tr>
          ) : (
            users.map((user) => {
              const canToggle = !disabled && canTogglePermission?.(user);
              const canSetPwd = !disabled && canSetPassword?.(user);
              const canRemove = !disabled && canRemoveUser?.(user);

              const isUpdatingPermission = updatingUserId === user.id;
              const isRemoving = removingUserId === user.id;
              const isSettingPassword = settingPasswordUserId === user.id;

              return (
                <tr key={user.id}>
                  <td>
                    <div className="permission-user">
                      <div className="permission-user__avatar">
                        {getUserInitials(user.username)}
                      </div>

                      <div className="permission-user__meta">
                        <div className="permission-user__name">{user.username}</div>

                        {user.isCurrentUser ? (
                          <div className="permission-user__badge">Bạn</div>
                        ) : user.isPrimary ? (
                          <div className="permission-user__badge">Primary</div>
                        ) : null}
                      </div>
                    </div>
                  </td>

                  <td>{user.systemRole || "--"}</td>
                  <td>{user.roleInHome || "--"}</td>
                  <td>{user.provider || "--"}</td>

                  <td>
                    <button
                      type="button"
                      className={`permission-switch ${
                        user.allowProfileActivation ? "is-on" : ""
                      }`}
                      disabled={!canToggle || isUpdatingPermission}
                      onClick={() => onTogglePermission?.(user.id)}
                      aria-pressed={user.allowProfileActivation}
                      aria-label={
                        user.allowProfileActivation
                          ? `Block ${user.username} from home`
                          : `Allow ${user.username} to home`
                      }
                      title={
                        user.allowProfileActivation
                          ? "Currently allowing access to home"
                          : "Currently blocking access to home"
                      }
                    >
                      <span className="permission-switch__thumb" />
                    </button>
                  </td>

                  <td>
                    <div className="permission-actions">
                      <button
                        type="button"
                        className="permission-action-button"
                        onClick={() => onSetPassword?.(user.id)}
                        disabled={!canSetPwd || isSettingPassword}
                      >
                        {isSettingPassword ? "Saving..." : "Set Password"}
                      </button>

                      <button
                        type="button"
                        className="permission-action-button permission-action-button--danger"
                        onClick={() => onRemoveUser?.(user.id)}
                        disabled={!canRemove || isRemoving}
                      >
                        {isRemoving ? "Removing..." : "Remove"}
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}

export default PermissionTable;