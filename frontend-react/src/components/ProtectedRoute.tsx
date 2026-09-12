import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <main className="min-h-screen flex items-center justify-center bg-gray-100">
        <p className="text-sm text-gray-500">Loading...</p>
      </main>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}