// frontend-react/src/pages/DashboardPage.tsx
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import PostFeed from "../components/PostFeed";

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  return (
    <main className="min-h-screen bg-gray-100 px-4 py-8">
      <div className="mx-auto mb-6 flex w-full max-w-lg items-center justify-between">
        <p className="text-sm text-gray-600">Signed in as {user?.username}</p>
        <div className="flex items-center gap-3">
          <Link to="/dashboard/profile" className="text-sm font-semibold text-gray-700 hover:text-gray-900">
            Edit profile
          </Link>
          <button onClick={handleLogout} className="text-sm font-semibold text-gray-700 hover:text-gray-900">
            Log out
          </button>
        </div>
      </div>

      <PostFeed />
    </main>
  );
}