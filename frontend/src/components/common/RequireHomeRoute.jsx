import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../providers/AuthProvider";

function RequireHomeRoute() {
  const { user, bootstrapping, isAuthenticated } = useAuth();

  if (bootstrapping) {
    return <div style={{ padding: 24 }}>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!user?.homeId) {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
}

export default RequireHomeRoute;