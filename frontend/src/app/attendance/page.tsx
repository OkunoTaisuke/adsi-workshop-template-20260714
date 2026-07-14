"use client";

import { useAuth } from "@/lib/auth-context";
import { Header } from "@/components/Header";
import { MonthPicker } from "@/components/attendance/MonthPicker";
import { AttendanceTable } from "@/components/attendance/AttendanceTable";
import { useEffect, useState } from "react";
import * as api from "@/lib/attendance-api";
import type { AttendanceDetailResponse } from "@/lib/types";

export default function AttendancePage() {
  const { employee, isLoading } = useAuth();
  const [year, setYear] = useState(() => new Date().getFullYear());
  const [month, setMonth] = useState(() => new Date().getMonth() + 1);
  const [records, setRecords] = useState<AttendanceDetailResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!employee) return;
    let cancelled = false;
    api
      .getMyAttendance(year, month)
      .then((data) => { if (!cancelled) { setRecords(data); setError(null); } })
      .catch(() => { if (!cancelled) { setRecords([]); setError("勤怠データの取得に失敗しました"); } })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [employee, year, month]);

  if (isLoading || !employee) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-500">読み込み中...</p>
      </div>
    );
  }

  const handleMonthChange = (y: number, m: number) => {
    setYear(y);
    setMonth(m);
  };

  const now = new Date();
  const isCurrentMonth = year === now.getFullYear() && month === now.getMonth() + 1;

  return (
    <>
      <Header />
      <main className="flex-1 p-6 bg-gray-50">
        <div className="max-w-5xl mx-auto">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-bold text-gray-900">勤怠一覧</h2>
            <MonthPicker year={year} month={month} onChange={handleMonthChange} />
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">
              {error}
            </div>
          )}

          <div className="bg-white rounded-lg border">
            {loading ? (
              <div className="p-8 text-center text-gray-400">読み込み中...</div>
            ) : (
              <AttendanceTable
                records={records}
                editable={isCurrentMonth}
              />
            )}
          </div>
        </div>
      </main>
    </>
  );
}
