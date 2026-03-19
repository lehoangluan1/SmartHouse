import "./ScheduleTable.css";

function ScheduleTable({ schedules, onEdit, onDelete, deletingId }) {
  return (
    <div className="settings-table-card">
      <table className="settings-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Time</th>
            <th>Mode</th>
            <th>Days</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          {schedules.length === 0 ? (
            <tr>
              <td colSpan="5" className="settings-table__empty">
                No schedules
              </td>
            </tr>
          ) : (
            schedules.map((schedule) => (
              <tr key={schedule.id}>
                <td>{schedule.name}</td>

                <td>
                  <div className="schedule-time">
                    <span className="schedule-time__chip">
                      {schedule.startTime}
                    </span>
                    <span className="schedule-time__to">to</span>
                    <span className="schedule-time__chip">
                      {schedule.endTime}
                    </span>
                    {schedule.overnight ? (
                      <span className="schedule-time__overnight">
                        (next day)
                      </span>
                    ) : null}
                  </div>
                </td>

                <td>
                  <span className="schedule-mode-badge">{schedule.mode}</span>
                </td>

                <td>{schedule.daysText}</td>

                <td>
                  <div className="schedule-actions">
                    <button
                      type="button"
                      className="schedule-actions__edit"
                      onClick={() => onEdit(schedule)}
                      disabled={deletingId === schedule.id}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="schedule-actions__delete"
                      onClick={() => onDelete(schedule.id)}
                      disabled={deletingId === schedule.id}
                    >
                      {deletingId === schedule.id ? "Deleting..." : "Delete"}
                    </button>
                  </div>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

export default ScheduleTable;