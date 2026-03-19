import { useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import "./AppLayout.css";

function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const currentUser = JSON.parse(localStorage.getItem("currentUser") || "{}");

  return (
    <div className="app-layout">
      <button
        type="button"
        className={`app-mobile-menu ${sidebarOpen ? "hidden" : ""}`}
        onClick={() => setSidebarOpen(true)}
        aria-label="Open menu"
      >
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M4 7h16M4 12h16M4 17h16"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        </svg>
      </button>

      <div className={`app-sidebar-wrap ${sidebarOpen ? "open" : ""}`}>
        <div
          className="app-sidebar-backdrop"
          onClick={() => setSidebarOpen(false)}
        />

        <div className="app-sidebar-panel">
          <Sidebar
            onNavigate={() => setSidebarOpen(false)}
            profile={{
              name: currentUser?.username || "User",
              initials: (currentUser?.username || "U").slice(0, 1).toUpperCase(),
            }}
          />

          <button
            type="button"
            className="app-sidebar-close"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close menu"
          >
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M6 6l12 12M18 6 6 18"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
            </svg>
          </button>
        </div>
      </div>

      <main className="app-page">
        <div className="app-page-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

export default AppLayout;