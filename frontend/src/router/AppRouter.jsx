import { Navigate, Route, Routes } from "react-router-dom";

import AppLayout from "../components/layout/AppLayout";
import ProtectedRoute from "../components/common/ProtectedRoute";
import RequireHomeRoute from "../components/common/RequireHomeRoute";
import RoleProtectedRoute from "../components/common/RoleProtecteedRoute";
import RouteErrorBoundary from "../components/common/RouteErrorBoundary";

import LoginPage from "../pages/auth/LoginPage";
import GoogleAuthCallbackPage from "../pages/auth/GoogleAuthCallbackPage";
import AdminCreateUserPage from "../pages/auth/AdminCreateUserPage";

import DashboardPage from "../pages/dashboard/DashboardPage";
import HistoryPage from "../pages/history/HistoryPage";
import SettingsPage from "../pages/settings/SettingsPage";
import AuditLogsPage from "../pages/audit/AuditLogsPage";
import ConfigPage from "../pages/config/ConfigPage";

import BadRequestPage from "../pages/errors/BadRequestPage";
import ForbiddenPage from "../pages/errors/ForbiddenPage";
import NotFoundPage from "../pages/errors/NotFoundPage";
import InternalServerErrorPage from "../pages/errors/InternalServerErrorPage";

function AppRouter() {
  return (
    <RouteErrorBoundary>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />

        <Route path="/login" element={<LoginPage />} />
        <Route path="/auth/google/callback" element={<GoogleAuthCallbackPage />} />

        <Route path="/400" element={<BadRequestPage />} />
        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="/404" element={<NotFoundPage />} />
        <Route path="/500" element={<InternalServerErrorPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route element={<RequireHomeRoute />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/history" element={<HistoryPage />} />
              <Route path="/configs" element={<ConfigPage />} />

              <Route
                element={
                  <RoleProtectedRoute homeRoles={["OWNER", "CO_OWNER"]} />
                }
              >
                <Route path="/settings" element={<SettingsPage />} />
              </Route>
            </Route>

            <Route
              element={
                <RoleProtectedRoute roles={["SUPER_ADMIN", "ADMIN"]} />
              }
            >
              <Route path="/audit-logs" element={<AuditLogsPage />} />
            </Route>
          </Route>

          <Route
            element={
              <RoleProtectedRoute roles={["SUPER_ADMIN", "ADMIN"]} />
            }
          >
            <Route path="/admin/users/create" element={<AdminCreateUserPage />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </RouteErrorBoundary>
  );
}

export default AppRouter;