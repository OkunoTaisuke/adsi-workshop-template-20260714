"use client";

import { useAuth } from "@/lib/auth-context";
import { Header } from "@/components/Header";
import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import * as api from "@/lib/attendance-api";
import type { BreakUpdateRequest } from "@/lib/types";

export default function EditAttendancePage() {
  const { employee, isLoading } = useAuth();
  const params = useParams();
  const router = useRouter();
  const id = Number(params.id);

  const [clockIn, setClockIn] = useState("");
  const [clockOut, setClockOut] = useState("");
  const [breaks, setBreaks] = useState<BreakUpdateRequest[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [originalDate, setOriginalDate] = useState<string>("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!employee || !id) return;

    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1;

    api
      .getMyAttendance(year, month)
      .then((records) => {
        const record = records.find((r) => r.id === id);
        if (record) {
          setClockIn(record.clockIn ? record.clockIn.substring(0, 5) : "");
          setClockOut(record.clockOut ? record.clockOut.substring(0, 5) : "");
          setBreaks(
            record.breaks.map((b) => ({
              id: b.id ?? undefined,
              breakStart: b.breakStart ? b.breakStart.substring(0, 5) : "",
              breakEnd: b.breakEnd ? b.breakEnd.substring(0, 5) : "",
            }))
          );
          setOriginalDate(record.date);
        }
      })
      .catch(() => setError("データの取得に失敗しました"))
      .finally(() => setLoading(false));
  }, [employee, id]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      await api.updateAttendance(id, {
        clockIn: clockIn || null,
        clockOut: clockOut || null,
        breaks: breaks.filter((b) => b.breakStart),
      });
      router.push("/attendance");
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "保存に失敗しました";
      setError(msg);
    } finally {
      setSaving(false);
    }
  };

  const addBreak = () => {
    setBreaks([...breaks, { breakStart: "12:00", breakEnd: "13:00" }]);
  };

  const removeBreak = (index: number) => {
    setBreaks(breaks.filter((_, i) => i !== index));
  };

  const updateBreak = (
    index: number,
    field: "breakStart" | "breakEnd",
    value: string
  ) => {
    const updated = [...breaks];
    updated[index] = { ...updated[index], [field]: value };
    setBreaks(updated);
  };

  if (isLoading || !employee || loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-500">読み込み中...</p>
      </div>
    );
  }

  return (
    <>
      <Header />
      <main className="flex-1 p-6 bg-gray-50">
        <div className="max-w-lg mx-auto">
          <h2 className="text-xl font-bold text-gray-900 mb-6">
            打刻修正 — {originalDate}
          </h2>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">
              {error}
            </div>
          )}

          <div className="bg-white rounded-lg border p-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                出勤時刻
              </label>
              <input
                type="time"
                value={clockIn}
                onChange={(e) => setClockIn(e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                退勤時刻
              </label>
              <input
                type="time"
                value={clockOut}
                onChange={(e) => setClockOut(e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-sm font-medium text-gray-700">
                  休憩
                </label>
                <button
                  onClick={addBreak}
                  className="text-sm text-blue-600 hover:text-blue-800"
                >
                  + 追加
                </button>
              </div>
              {breaks.map((b, i) => (
                <div key={i} className="flex items-center gap-2 mb-2">
                  <input
                    type="time"
                    value={b.breakStart}
                    onChange={(e) => updateBreak(i, "breakStart", e.target.value)}
                    className="border border-gray-300 rounded px-2 py-1 text-sm"
                  />
                  <span className="text-gray-400">-</span>
                  <input
                    type="time"
                    value={b.breakEnd || ""}
                    onChange={(e) => updateBreak(i, "breakEnd", e.target.value)}
                    className="border border-gray-300 rounded px-2 py-1 text-sm"
                  />
                  <button
                    onClick={() => removeBreak(i)}
                    className="text-red-400 hover:text-red-600 text-sm"
                  >
                    削除
                  </button>
                </div>
              ))}
            </div>

            <div className="flex gap-3 pt-4">
              <button
                onClick={handleSave}
                disabled={saving}
                className="px-5 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
              >
                保存
              </button>
              <button
                onClick={() => router.push("/attendance")}
                className="px-5 py-2 border border-gray-300 rounded text-gray-600 hover:bg-gray-50"
              >
                キャンセル
              </button>
            </div>
          </div>
        </div>
      </main>
    </>
  );
}
