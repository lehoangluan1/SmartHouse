import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../providers/AuthProvider";

function ProtectedRoute() {
  const location = useLocation();
  const { isAuthenticated, bootstrapping, user } = useAuth();
  console.log(user);
  if (bootstrapping) {
    return <div style={{ padding: 24 }}>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (user?.status && ["INACTIVE", "LOCKED"].includes(user.status)) {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
}

export default ProtectedRoute;