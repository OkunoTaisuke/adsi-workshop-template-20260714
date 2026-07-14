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

export default function ManagerLeavesPage() {
  const { employee, hasRole } = useAuth();
  const [leaves, setLeaves] = useState<LeaveResponse[]>([]);
  const [rejectId, setRejectId] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    if (!hasRole("MANAGER")) return;
    let active = true;
    async function loadLeaves() {
      try {
        const data = await apiFetch<LeaveResponse[]>("/leaves/subordinates");
        if (active) setLeaves(data);
      } catch {
        /* ignore */
      }
    }
    loadLeaves();
    return () => { active = false; };
  }, [refreshKey, hasRole]);

  const handleApprove = async (id: number) => {
    try {
      await apiFetch(`/leaves/${id}/approve`, { method: "PUT" });
      setRefreshKey((k) => k + 1);
    } catch {
      /* ignore */
    }
  };

  const handleReject = async () => {
    if (rejectId === null) return;
    try {
      await apiFetch(`/leaves/${rejectId}/reject`, {
        method: "PUT",
        body: JSON.stringify({ reason: rejectReason || null }),
      });
      setRejectId(null);
      setRejectReason("");
      setRefreshKey((k) => k + 1);
    } catch {
      /* ignore */
    }
  };

  if (!employee || !hasRole("MANAGER")) {
    return <div className="p-6 text-gray-500">アクセス権がありません</div>;
  }

  return (
    <div className="p-6">
      <h1 className="text-xl font-bold mb-4">休暇承認（同一部署メンバーの申請）</h1>

      {rejectId !== null && (
        <div className="mb-4 p-4 border rounded bg-gray-50">
          <p className="text-sm font-medium mb-2">却下理由（任意）</p>
          <input
            type="text"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            maxLength={500}
            className="w-full px-3 py-2 border rounded text-sm mb-2"
          />
          <div className="flex gap-2">
            <button onClick={handleReject} className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 text-sm">
              却下する
            </button>
            <button onClick={() => { setRejectId(null); setRejectReason(""); }} className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400 text-sm">
              戻る
            </button>
          </div>
        </div>
      )}

      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="border-b bg-gray-50">
            <th className="px-3 py-2 text-left">申請者</th>
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
              <td className="px-3 py-2">{leave.employeeName}</td>
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
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleApprove(leave.id)}
                      className="px-2 py-1 bg-green-600 text-white rounded text-xs hover:bg-green-700"
                    >
                      承認
                    </button>
                    <button
                      onClick={() => setRejectId(leave.id)}
                      className="px-2 py-1 bg-red-600 text-white rounded text-xs hover:bg-red-700"
                    >
                      却下
                    </button>
                  </div>
                )}
              </td>
            </tr>
          ))}
          {leaves.length === 0 && (
            <tr>
              <td colSpan={6} className="px-3 py-4 text-center text-gray-500">
                申請がありません
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
