"use client";

import type { AttendanceResponse } from "@/lib/types";

interface TodayStatusProps {
  attendance: AttendanceResponse | null;
}

function formatTime(time: string | null): string {
  if (!time) return "-";
  return time.substring(0, 5);
}

function formatMinutes(minutes: number | null): string {
  if (minutes == null) return "-";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}:${String(m).padStart(2, "0")}`;
}

export function TodayStatus({ attendance }: TodayStatusProps) {
  if (!attendance || !attendance.clockIn) {
    return (
      <div className="text-gray-500 text-sm">まだ出勤していません</div>
    );
  }

  const getStateLabel = () => {
    if (attendance.clockOut) return "退勤済み";
    const hasActiveBreak = attendance.breaks.some((b) => b.breakEnd === null);
    if (hasActiveBreak) return "休憩中";
    return "勤務中";
  };

  return (
    <div className="space-y-2 text-sm">
      <div className="flex items-center gap-2">
        <span className="text-gray-500">状態:</span>
        <span className="font-semibold">{getStateLabel()}</span>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-gray-500">出勤:</span>
        <span>{formatTime(attendance.clockIn)}</span>
      </div>
      {attendance.clockOut && (
        <div className="flex items-center gap-2">
          <span className="text-gray-500">退勤:</span>
          <span>{formatTime(attendance.clockOut)}</span>
        </div>
      )}
      {attendance.breaks.length > 0 && (
        <div className="flex items-center gap-2">
          <span className="text-gray-500">休憩:</span>
          <span>
            {attendance.breaks.map((b, i) => (
              <span key={i} className="mr-2">
                {formatTime(b.breakStart)}-{formatTime(b.breakEnd)}
              </span>
            ))}
          </span>
        </div>
      )}
      {attendance.totalWorkMinutes != null && (
        <div className="flex items-center gap-2">
          <span className="text-gray-500">勤務時間:</span>
          <span>{formatMinutes(attendance.totalWorkMinutes)}</span>
          {attendance.overtimeMinutes != null && attendance.overtimeMinutes > 0 && (
            <span className="text-orange-500 text-xs">
              (残業 {formatMinutes(attendance.overtimeMinutes)})
            </span>
          )}
        </div>
      )}
    </div>
  );
}
