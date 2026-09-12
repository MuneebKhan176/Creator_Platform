import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  return (
    <main className="min-h-screen flex items-center justify-center bg-gray-100 px-4">
      <div className="w-full max-w-sm rounded-xl border border-gray-200 bg-white p-8 shadow-sm">
        <h1 className="text-xl font-semibold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-sm text-gray-600">Signed in as {user?.username}</p>
        <p className="mt-1 text-xs text-gray-400">Roles: {user?.roles.join(", ")}</p>
        <button
          onClick={handleLogout}
          className="mt-5 w-full rounded-md bg-gray-800 py-2.5 text-sm font-semibold text-white transition hover:bg-gray-900"
        >
          Log out
        </button>
      </div>
    </main>
  );
}