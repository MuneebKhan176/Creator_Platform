// Base URL of the Spring Boot backend.
export const API_BASE_URL = "http://localhost:4000";

export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data?: T;
}

/** Set a bar open
 * POSTs JSON to the backend and returns the parsed ApiResponse.
 * Includes credentials so the HttpOnly auth cookie can be set after verification.
 */
export async function apiPost<T = unknown>(path: string, body: unknown): Promise<ApiResponse<T>> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(body),
    });
  } catch (networkError) {
    throw new Error("Could not reach the server. Please check your connection and try again.");
  }

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
