// frontend-react/src/api/client.ts — only the addition at the bottom is new,
// everything above is unchanged from what you sent.
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

function withCsrfHeader(headers: Record<string, string>): Record<string, string> {
  const csrfToken = readCookie("csrf_token");
  return csrfToken ? { ...headers, "X-CSRF-Token": csrfToken } : headers;
}

async function refreshAccessToken(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: "POST",
      credentials: "include",
    });

    return response.ok;
  } catch {
    return false;
  }
}

async function request<T = unknown>(
  path: string,
  options: RequestInit,
  retryAfterRefresh = true
): Promise<ApiResponse<T>> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      credentials: "include",
    });
  } catch {
    throw new Error("Could not reach the server. Please check your connection and try again.");
  }

  // Access tokens are short-lived. If one has expired, use the HttpOnly
  // refresh token to obtain a new access token and retry the original request once.
  if (
    response.status === 401 &&
    retryAfterRefresh &&
    path !== "/api/v1/auth/refresh" &&
    path !== "/api/v1/auth/login" &&
    path !== "/api/v1/auth/register" &&
    path !== "/api/v1/auth/verify-email" &&
    path !== "/api/v1/auth/resend-verification" &&
    path !== "/api/v1/auth/forgot-password" &&
    path !== "/api/v1/auth/reset-password"
  ) {
    const refreshed = await refreshAccessToken();

    if (refreshed) {
      return request<T>(path, options, false);
    }
  }

  return parseResponse<T>(response);
}

export async function apiPost<T = unknown>(path: string, body: unknown): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "POST",
    headers: withCsrfHeader({ "Content-Type": "application/json" }),
    body: JSON.stringify(body),
  });
}

export async function apiGet<T = unknown>(path: string): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "GET",
  });
}

export async function apiDelete<T = unknown>(path: string): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "DELETE",
    headers: withCsrfHeader({}),
  });
}

export async function apiPut<T = unknown>(path: string, body: unknown): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "PUT",
    headers: withCsrfHeader({ "Content-Type": "application/json" }),
    body: JSON.stringify(body),
  });
}

export async function apiUpload<T = unknown>(path: string, file: File): Promise<ApiResponse<T>> {
  const formData = new FormData();
  formData.append("file", file);

<<<<<<< HEAD
  return request<T>(path, {
    method: "POST",
    // No Content-Type here on purpose — the browser sets
    // multipart/form-data with the correct boundary itself.
    headers: withCsrfHeader({}),
    body: formData,
  });
=======
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: "POST",
      headers: withCsrfHeader({}),
      credentials: "include",
      body: formData,
    });
  } catch {
    throw new Error("Could not reach the server. Please check your connection and try again.");
  }
  return parseResponse<T>(response);
}

// NEW — mixed text-field + multiple-file submissions (e.g. post content +
// images + video). Same pattern as apiUpload: no Content-Type header so the
// browser sets the multipart boundary itself.
export async function apiPostForm<T = unknown>(path: string, formData: FormData): Promise<ApiResponse<T>> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: "POST",
      headers: withCsrfHeader({}),
      credentials: "include",
      body: formData,
    });
  } catch {
    throw new Error("Could not reach the server. Please check your connection and try again.");
  }
  return parseResponse<T>(response);
>>>>>>> 4405f6b (Added Profile Service and Like/Comment)
}