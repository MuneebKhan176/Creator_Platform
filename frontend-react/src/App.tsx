import { Routes, Route, Navigate } from "react-router-dom";
import RegisterPage from "./pages/RegisterPage";
import VerifyPage from "./pages/VerifyPage";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import ProtectedRoute from "./components/ProtectedRoute";
import { AuthProvider } from "./context/AuthContext";

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Navigate to="/register" replace />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify" element={<VerifyPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<DashboardPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  );
}