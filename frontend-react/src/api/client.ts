export const API_BASE_URL = "http://localhost:4000";

export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data?: T;
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

export async function apiPost<T = unknown>(path: string, body: unknown): Promise<ApiResponse<T>> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
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