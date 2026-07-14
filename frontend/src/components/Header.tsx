"use client";

import { useAuth } from "@/lib/auth-context";
import Link from "next/link";

export function Header() {
  const { employee, logout, hasRole } = useAuth();

  const handleLogout = () => {
    logout();
    window.location.href = window.location.pathname.replace(/\/$/, "") + "/login";
  };

  if (!employee) return null;

  const roleLabel = hasRole("ADMIN")
    ? "管理者"
    : hasRole("MANAGER")
      ? "上司"
      : "社員";

  return (
    <header className="bg-white border-b border-gray-200">
      <div className="px-6 py-3 flex items-center justify-between">
        <Link
          href="/"
          className="text-lg font-semibold text-gray-900 hover:text-blue-600"
        >
          勤怠管理システム
        </Link>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">{employee.name}</span>
          <span className="text-xs px-2 py-0.5 rounded bg-gray-100 text-gray-500">
            {roleLabel}
          </span>
          <button
            onClick={handleLogout}
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            ログアウト
          </button>
        </div>
      </div>
      <nav className="px-6 pb-2 flex gap-4">
        <Link href="/" className="text-sm text-gray-600 hover:text-blue-600">
          ホーム
        </Link>
        <Link href="/leaves" className="text-sm text-gray-600 hover:text-blue-600">
          休暇申請
        </Link>
        {hasRole("MANAGER") && (
          <Link href="/manager/leaves" className="text-sm text-gray-600 hover:text-blue-600">
            休暇承認
          </Link>
        )}
        {hasRole("ADMIN") && (
          <>
            <Link href="/admin/departments" className="text-sm text-gray-600 hover:text-blue-600">
              部署管理
            </Link>
            <Link href="/admin/employees" className="text-sm text-gray-600 hover:text-blue-600">
              社員管理
            </Link>
          </>
        )}
      </nav>
    </header>
  );
}
