"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import { apiFetch } from "./api-client";

interface Employee {
  id: number;
  email: string;
  name: string;
  role: "EMPLOYEE" | "ADMIN";
}

interface AuthContextType {
  employee: Employee | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const init = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          if (!cancelled) setIsLoading(false);
          return;
        }
        const emp = await apiFetch<Employee>("/auth/me");
        if (!cancelled) setEmployee(emp);
      } catch {
        localStorage.removeItem("token");
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };

    init();

    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiFetch<{ token: string; employee: Employee }>(
      "/auth/login",
      {
        method: "POST",
        body: JSON.stringify({ email, password }),
      }
    );
    localStorage.setItem("token", response.token);
    setEmployee(response.employee);
    setIsLoading(false);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("token");
    setEmployee(null);
  }, []);

  return (
    <AuthContext.Provider value={{ employee, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
