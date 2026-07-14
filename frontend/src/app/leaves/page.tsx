"use client";

import { useAuth } from "@/lib/auth-context";
import { apiFetch } from "@/lib/api-client";
import { useEffect, useState } from "react";

interface LeaveResponse {
  id: number;
  employeeId: number;
  employeeName: string;
  leaveType: string;
  leavePeriodType: string;
  leavePeriodTypeDisplay: string;
  startDate: string;
  endDate: string;
  reason: string | null;
  status: string;
  statusDisplay: string;
  approvedByName: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  createdAt: string;
}

export default function LeavesPage() {
  const { employee } = useAuth();
  const [leaves, setLeaves] = useState<LeaveResponse[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [leavePeriodType, setLeavePeriodType] = useState("FULL_DAY");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let active = true;
    async function loadLeaves() {
      try {
        const data = await apiFetch<LeaveResponse[]>("/leaves/my");
        if (active) setLeaves(data);
      } catch {
        /* ignore */
      }
    }
    loadLeaves();
    return () => { active = false; };
  }, [refreshKey]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      await apiFetch("/leaves", {
        method: "POST",
        body: JSON.stringify({
          leaveType: "PAID",
          leavePeriodType,
          startDate,
          endDate: endDate || startDate,
          reason: reason || null,
        }),
      });
      setShowForm(false);
      setStartDate("");
      setEndDate("");
      setReason("");
      setRefreshKey((k) => k + 1);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "エラーが発生しました");
    }
  };

  const handleCancel = async (id: number) => {
    try {
      await apiFetch(`/leaves/${id}/cancel`, { method: "PUT" });
      setRefreshKey((k) => k + 1);
    } catch {
      /* ignore */
    }
  };

  if (!employee) return null;

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-bold">休暇申請</h1>
        <button
          onClick={() => setShowForm(true)}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
        >
          + 新規申請
        </button>
      </div>

      {showForm && (
        <div className="mb-6 p-4 border rounded bg-gray-50">
          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-gray-700">種別</label>
              <input type="text" value="有給休暇" disabled className="mt-1 w-full px-3 py-2 border rounded bg-gray-100 text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">区分</label>
              <div className="flex gap-4 mt-1">
                {[
                  { value: "FULL_DAY", label: "全日" },
                  { value: "AM_ONLY", label: "午前休" },
                  { value: "PM_ONLY", label: "午後休" },
                ].map((opt) => (
                  <label key={opt.value} className="flex items-center gap-1 text-sm">
                    <input
                      type="radio"
                      name="periodType"
                      value={opt.value}
                      checked={leavePeriodType === opt.value}
                      onChange={(e) => setLeavePeriodType(e.target.value)}
                    />
                    {opt.label}
                  </label>
                ))}
              </div>
            </div>
            <div className="flex gap-4">
              <div className="flex-1">
                <label className="block text-sm font-medium text-gray-700">開始日</label>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required
                  className="mt-1 w-full px-3 py-2 border rounded text-sm"
                />
              </div>
              <div className="flex-1">
                <label className="block text-sm font-medium text-gray-700">終了日</label>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="mt-1 w-full px-3 py-2 border rounded text-sm"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">理由（任意）</label>
              <input
                type="text"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                maxLength={500}
                className="mt-1 w-full px-3 py-2 border rounded text-sm"
              />
            </div>
            {error && <p className="text-red-600 text-sm">{error}</p>}
            <div className="flex gap-2">
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm">
                申請
              </button>
              <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400 text-sm">
                キャンセル
              </button>
            </div>
          </form>
        </div>
      )}

      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="border-b bg-gray-50">
            <th className="px-3 py-2 text-left">種別</th>
            <th className="px-3 py-2 text-left">区分</th>
            <th className="px-3 py-2 text-left">期間</th>
            <th className="px-3 py-2 text-left">状態</th>
            <th className="px-3 py-2 text-left">操作</th>
          </tr>
        </thead>
        <tbody>
          {leaves.map((leave) => (
            <tr key={leave.id} className="border-b">
              <td className="px-3 py-2">有給</td>
              <td className="px-3 py-2">{leave.leavePeriodTypeDisplay}</td>
              <td className="px-3 py-2">
                {leave.startDate} ~ {leave.endDate}
              </td>
              <td className="px-3 py-2">
                <span
                  className={`px-2 py-0.5 rounded text-xs ${
                    leave.status === "APPROVED"
                      ? "bg-green-100 text-green-800"
                      : leave.status === "PENDING"
                        ? "bg-yellow-100 text-yellow-800"
                        : leave.status === "REJECTED"
                          ? "bg-red-100 text-red-800"
                          : "bg-gray-100 text-gray-800"
                  }`}
                >
                  {leave.statusDisplay}
                </span>
              </td>
              <td className="px-3 py-2">
                {leave.status === "PENDING" && (
                  <button
                    onClick={() => handleCancel(leave.id)}
                    className="text-red-600 hover:text-red-800 text-xs"
                  >
                    取消
                  </button>
                )}
              </td>
            </tr>
          ))}
          {leaves.length === 0 && (
            <tr>
              <td colSpan={5} className="px-3 py-4 text-center text-gray-500">
                申請がありません
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
