import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../../providers/AuthProvider";
import "./Sidebar.css";

function buildMenus(user) {
  const items = [
    { label: "Dashboard", path: "/dashboard" },
    { label: "History", path: "/history" },
    { label: "Configs", path: "/configs" },
  ];

  if (["OWNER", "CO_OWNER"].includes(user?.roleInHome)) {
    items.push({ label: "System Settings", path: "/settings" });
  }

  if (["SUPER_ADMIN", "ADMIN"].includes(user?.role)) {
    items.push({ label: "Audit Logs", path: "/audit-logs" });
  }

  return items;
}
function Sidebar({ profile, onNavigate }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const menus = buildMenus(user);
  console.log(user);
  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <div className="sidebar__brand-icon">
          <svg viewBox="0 0 24 24" fill="none">
            <path
              d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-4.5v-6h-5v6H5a1 1 0 0 1-1-1v-9.5Z"
              stroke="currentColor"
              strokeWidth="1.7"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <span>Smart House</span>
      </div>

      <nav className="sidebar__nav">
        {menus.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end
            className={({ isActive }) =>
              `sidebar__nav-item ${isActive ? "active" : ""}`
            }
            onClick={onNavigate}
          >
            <span className="sidebar__nav-icon">
              <svg viewBox="0 0 24 24" fill="none">
                <path
                  d="M5 12h14M5 7h14M5 17h14"
                  stroke="currentColor"
                  strokeWidth="1.7"
                  strokeLinecap="round"
                />
              </svg>
            </span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar__bottom">
        <div className="sidebar__profile">
          <div className="sidebar__avatar">
            {user?.username?.slice(0, 1)?.toUpperCase() || profile?.initials || "U"}
          </div>
          <div className="sidebar__profile-name">
            {user?.username || profile?.name || "User"}
          </div>
        </div>

        <button type="button" className="sidebar__logout" onClick={handleLogout}>
          <span className="sidebar__logout-icon">
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M10 17l-5-5 5-5M5 12h10M14 5h3a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-3"
                stroke="currentColor"
                strokeWidth="1.7"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </span>
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
}

export default Sidebar;