import { createContext, useCallback, useContext, useEffect, useState, ReactNode } from "react";
import { apiGet, apiPost } from "../api/client";

export interface AuthUser {
  id: number;
  username: string;
  email: string;
  emailVerified: boolean;
  accountStatus: "ACTIVE" | "SUSPENDED" | "DISABLED";
  roles: string[];
}

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  refresh: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const result = await apiGet<AuthUser>("/api/v1/auth/me");
      setUser(result.data ?? null);
    } catch {
      // 401 (or network error) just means "not logged in" here.
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const logout = useCallback(async () => {
    try {
      await apiPost("/api/v1/auth/logout", {});
    } finally {
      setUser(null);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, refresh, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}