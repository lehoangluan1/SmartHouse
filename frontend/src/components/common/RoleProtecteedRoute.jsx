import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../providers/AuthProvider";

function RoleProtectedRoute({ roles = [], homeRoles = [] }) {
  const { user, bootstrapping, isAuthenticated } = useAuth();

  if (bootstrapping) {
    return <div style={{ padding: 24 }}>Loading...</div>;
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  const allowBySystemRole = !roles.length || roles.includes(user.role);
  const allowByHomeRole = !homeRoles.length || homeRoles.includes(user.roleInHome);

  if (!allowBySystemRole || !allowByHomeRole) {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
}

export default RoleProtectedRoute;