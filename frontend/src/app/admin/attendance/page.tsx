"use client";

import { useAuth } from "@/lib/auth-context";
import { Header } from "@/components/Header";
import { MonthPicker } from "@/components/attendance/MonthPicker";
import { AttendanceTable } from "@/components/attendance/AttendanceTable";
import { useEffect, useState } from "react";
import * as api from "@/lib/attendance-api";
import type { AttendanceDetailResponse, DepartmentResponse } from "@/lib/types";

export default function AdminAttendancePage() {
  const { employee, isLoading } = useAuth();
  const [year, setYear] = useState(() => new Date().getFullYear());
  const [month, setMonth] = useState(() => new Date().getMonth() + 1);
  const [departmentId, setDepartmentId] = useState<number | undefined>(undefined);
  const [employeeName, setEmployeeName] = useState("");
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [records, setRecords] = useState<AttendanceDetailResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.getDepartments().then(setDepartments).catch(() => setDepartments([]));
  }, []);

  useEffect(() => {
    if (!employee) return;
    let cancelled = false;
    api
      .getAllAttendance(year, month, departmentId, employeeName || undefined)
      .then((data) => { if (!cancelled) { setRecords(data); setError(null); } })
      .catch(() => { if (!cancelled) { setRecords([]); setError("勤怠データの取得に失敗しました"); } })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [employee, year, month, departmentId, employeeName]);

  if (isLoading || !employee) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-500">読み込み中...</p>
      </div>
    );
  }

  if (employee.role !== "ADMIN") {
    return (
      <>
        <Header />
        <main className="flex-1 p-6">
          <p className="text-red-600">この画面にアクセスする権限がありません</p>
        </main>
      </>
    );
  }

  return (
    <>
      <Header />
      <main className="flex-1 p-6 bg-gray-50">
        <div className="max-w-6xl mx-auto">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-bold text-gray-900">全社員勤怠一覧</h2>
            <MonthPicker
              year={year}
              month={month}
              onChange={(y, m) => {
                setYear(y);
                setMonth(m);
              }}
            />
          </div>

          <div className="flex gap-4 mb-4">
            <select
              value={departmentId ?? ""}
              onChange={(e) =>
                setDepartmentId(e.target.value ? Number(e.target.value) : undefined)
              }
              className="border border-gray-300 rounded px-3 py-2 text-sm"
            >
              <option value="">全部署</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>

            <input
              type="text"
              placeholder="社員名で絞り込み"
              value={employeeName}
              onChange={(e) => setEmployeeName(e.target.value)}
              className="border border-gray-300 rounded px-3 py-2 text-sm w-48"
            />
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">
              {error}
            </div>
          )}

          <div className="bg-white rounded-lg border">
            {loading ? (
              <div className="p-8 text-center text-gray-400">読み込み中...</div>
            ) : records.length === 0 ? (
              <div className="p-8 text-center text-gray-400">
                データがありません
              </div>
            ) : (
              <AttendanceTable records={records} showEmployee />
            )}
          </div>
        </div>
      </main>
    </>
  );
}
