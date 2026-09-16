export const API_BASE_URL = "http://localhost:4000";

export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data?: T;
}

function readCookie(name: string): string | null {
  const match = document.cookie.match(
    new RegExp(`(?:^|; )${name}=([^;]*)`)
  );

  return match ? decodeURIComponent(match[1]) : null;
}

async function parseResponse<T>(
  response: Response
): Promise<ApiResponse<T>> {
  let parsed: ApiResponse<T>;

  try {
    parsed = await response.json();
  } catch {
    throw new Error(
      "Unexpected server response. Please try again."
    );
  }

  if (!response.ok || !parsed.success) {
    throw new Error(
      parsed.message || "Something went wrong. Please try again."
    );
  }

  return parsed;
}

function withCsrfHeader(
  headers: Record<string, string>
): Record<string, string> {
  const csrfToken = readCookie("csrf_token");

  return csrfToken
    ? { ...headers, "X-CSRF-Token": csrfToken }
    : headers;
}

async function refreshAccessToken(): Promise<boolean> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/v1/auth/refresh`,
      {
        method: "POST",
        credentials: "include",
      }
    );

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
    throw new Error(
      "Could not reach the server. Please check your connection and try again."
    );
  }

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

export async function apiPost<T = unknown>(
  path: string,
  body: unknown
): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "POST",
    headers: withCsrfHeader({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify(body),
  });
}

export async function apiGet<T = unknown>(
  path: string
): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "GET",
  });
}

export async function apiDelete<T = unknown>(
  path: string
): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "DELETE",
    headers: withCsrfHeader({}),
  });
}

export async function apiPut<T = unknown>(
  path: string,
  body: unknown
): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "PUT",
    headers: withCsrfHeader({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify(body),
  });
}

/*
 * POST multipart/form-data.
 *
 * Used by posts and any other endpoint that needs
 * to send files together with normal form fields.
 *
 * Do NOT manually set Content-Type here.
 * The browser adds multipart/form-data and its boundary.
 */
export async function apiPostForm<T = unknown>(
  path: string,
  formData: FormData
): Promise<ApiResponse<T>> {
  return request<T>(path, {
    method: "POST",
    headers: withCsrfHeader({}),
    body: formData,
  });
}

/*
 * Upload a single file.
 *
 * Kept for existing functionality that uploads
 * one file using the "file" field.
 */
export async function apiUpload<T = unknown>(
  path: string,
  file: File
): Promise<ApiResponse<T>> {
  const formData = new FormData();

  formData.append("file", file);

  return apiPostForm<T>(path, formData);
}