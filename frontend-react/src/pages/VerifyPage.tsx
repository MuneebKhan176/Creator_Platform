import { FormEvent, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { apiPost } from "../api/client";

type State = "idle" | "verifying" | "success" | "error";

export default function VerifyPage() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState(searchParams.get("email") ?? "");
  const [code, setCode] = useState("");
  const [state, setState] = useState<State>("idle");
  const [message, setMessage] = useState("");

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    const trimmedEmail = email.trim();
    const trimmedCode = code.trim();

    if (!trimmedEmail) {
      setState("error");
      setMessage("Email is required.");
      return;
    }
    if (!/^\d{6}$/.test(trimmedCode)) {
      setState("error");
      setMessage("Enter the 6-digit code from your email.");
      return;
    }

    setState("verifying");
    setMessage("Verifying...");
    try {
      // change the endpoint:
      const result = await apiPost("/api/v1/auth/verify-email", {
      email: trimmedEmail,
      code: trimmedCode,
    });
      setState("success");
      setMessage(result.message);
    }
     catch (err) {
      const text = err instanceof Error ? err.message : "Registration failed.";
      setState("error");
      setMessage(text);
    }
  }

  const isBusy = state === "verifying" || state === "success";

  return (
    <main className="min-h-screen flex items-center justify-center bg-gray-100 px-4">
      <div className="w-full max-w-sm rounded-xl border border-gray-200 bg-white p-8 shadow-sm">
        <h1 className="text-xl font-semibold text-gray-900">Verify your email</h1>
        <p className="mt-1 text-sm text-gray-500">
          Enter the 6-digit code we sent to your email address.
        </p>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-1" noValidate>
          <label htmlFor="email" className="text-sm font-semibold text-gray-700">
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

          <label htmlFor="code" className="mt-3 text-sm font-semibold text-gray-700">
            Verification Code
          </label>
          <input
            id="code"
            type="text"
            inputMode="numeric"
            pattern="\d{6}"
            maxLength={6}
            autoComplete="one-time-code"
            placeholder="000000"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-sm tracking-widest outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          {state !== "idle" && (
            <div
              className={`mt-4 rounded-md px-3 py-2 text-sm ${
                state === "error"
                  ? "bg-red-50 text-red-700"
                  : state === "success"
                    ? "bg-green-50 text-green-700"
                    : "bg-blue-50 text-blue-700"
              }`}
            >
              {message}
            </div>
          )}

          <button
            type="submit"
            disabled={isBusy}
            className="mt-5 rounded-md bg-blue-600 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-300"
          >
            {state === "verifying" ? "Verifying..." : state === "success" ? "Verified" : "Verify"}
          </button>
        </form>
      </div>
    </main>
  );
}
