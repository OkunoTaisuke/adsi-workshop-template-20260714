"use client";

import { useAuth } from "@/lib/auth-context";
import { useEffect } from "react";

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const { employee, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && !employee) {
      const loginPath = window.location.pathname.replace(/\/$/, "") + "/login";
      window.location.href = loginPath;
    }
  }, [employee, isLoading]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-500">読み込み中...</p>
      </div>
    );
  }

  if (!employee) {
    return null;
  }

  return <>{children}</>;
}
