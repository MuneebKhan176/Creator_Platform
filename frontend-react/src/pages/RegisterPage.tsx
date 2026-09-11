import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiPost } from "../api/client";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 8;

type MessageKind = "error" | "success" | null;

export default function RegisterPage() {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ text: string; kind: MessageKind }>({
    text: "",
    kind: null,
  });

  // Frontend validation is a UX convenience only; the backend re-validates everything.
  function validate(): string | null {
    if (!username.trim()) return "Username is required.";
    if (!email.trim()) return "Email is required.";
    if (!EMAIL_REGEX.test(email.trim())) return "Please enter a valid email address.";
    if (!password) return "Password is required.";
    if (password.length < MIN_PASSWORD_LENGTH) {
      return `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
    }
    if (!confirmPassword) return "Please confirm your password.";
    if (password !== confirmPassword) return "Passwords do not match.";
    return null;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setMessage({ text: "", kind: null });

    const validationError = validate();
    if (validationError) {
      setMessage({ text: validationError, kind: "error" });
      return;
    }

    setLoading(true);
    try {
      const result = await apiPost("/api/auth/register", {
        username: username.trim(),
        email: email.trim(),
        password,
        confirmPassword,
      });

      setMessage({ text: result.message, kind: "success" });
      navigate(`/verify?email=${encodeURIComponent(email.trim())}`);
    } catch (err) {
      const text = err instanceof Error ? err.message : "Registration failed.";
      setMessage({ text, kind: "error" });
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="min-h-screen flex items-center justify-center bg-gray-100 px-4">
      <div className="w-full max-w-sm rounded-xl border border-gray-200 bg-white p-8 shadow-sm">
        <h1 className="text-xl font-semibold text-gray-900">Create an account</h1>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-1" noValidate>
          <label htmlFor="username" className="mt-3 text-sm font-semibold text-gray-700">
            Username
          </label>
          <input
            id="username"
            type="text"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          <label htmlFor="email" className="mt-3 text-sm font-semibold text-gray-700">
            Email
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          <label htmlFor="password" className="mt-3 text-sm font-semibold text-gray-700">
            Password
          </label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          <label htmlFor="confirmPassword" className="mt-3 text-sm font-semibold text-gray-700">
            Confirm Password
          </label>
          <input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          {message.kind && (
            <div
              className={`mt-4 rounded-md px-3 py-2 text-sm ${
                message.kind === "error"
                  ? "bg-red-50 text-red-700"
                  : "bg-green-50 text-green-700"
              }`}
            >
              {message.text}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="mt-5 rounded-md bg-blue-600 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-300"
          >
            {loading ? "Registering..." : "Register"}
          </button>
        </form>

        <p className="mt-5 text-center text-sm text-gray-500">
          Already have an account?{" "}
          <a href="/login" className="text-blue-600 hover:underline">
            Log in
          </a>
        </p>
      </div>
    </main>
  );
}
