export const API_BASE_URL = "http://localhost:4000";

export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data?: T;
}

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

async function parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
  let parsed: ApiResponse<T>;
  try {
    parsed = await response.json();
  } catch {
    throw new Error("Unexpected server response. Please try again.");
  }

  if (!response.ok || !parsed.success) {
    throw new Error(parsed.message || "Something went wrong. Please try again.");
  }

  return parsed;
}

// The csrf_token cookie is set by the backend (non-HttpOnly, on purpose) once
// logged in. Echoing it back as a header is what the backend's double-submit
// CSRF check compares against the cookie value.
function withCsrfHeader(headers: Record<string, string>): Record<string, string> {
  const csrfToken = readCookie("csrf_token");
  return csrfToken ? { ...headers, "X-CSRF-Token": csrfToken } : headers;
}

export async function apiPost<T = unknown>(path: string, body: unknown): Promise<ApiResponse<T>> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: "POST",
      headers: withCsrfHeader({ "Content-Type": "application/json" }),
      credentials: "include",
      body: JSON.stringify(body),
    });
  } catch {
    throw new Error("Could not reach the server. Please check your connection and try again.");
  }
  return parseResponse<T>(response);
}

export async function apiGet<T = unknown>(path: string): Promise<ApiResponse<T>> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: "GET",
      credentials: "include",
    });
  } catch {
    throw new Error("Could not reach the server. Please check your connection and try again.");
  }
  return parseResponse<T>(response);
}

export async function apiDelete<T = unknown>(path: string): Promise<ApiResponse<T>> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: "DELETE",
      headers: withCsrfHeader({}),
      credentials: "include",
    });
  } catch {
    throw new Error("Could not reach the server. Please check your connection and try again.");
  }
  return parseResponse<T>(response);
}