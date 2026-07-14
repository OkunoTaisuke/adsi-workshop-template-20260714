"use client";

import Link from "next/link";
import type { AttendanceDetailResponse } from "@/lib/types";
import { StatusIcon } from "./StatusIcon";

interface AttendanceTableProps {
  records: AttendanceDetailResponse[];
  showEmployee?: boolean;
  editable?: boolean;
}

const WEEKDAYS = ["日", "月", "火", "水", "木", "金", "土"];

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

function getDayOfWeek(dateStr: string): number {
  return new Date(dateStr).getDay();
}

function getRowClass(dateStr: string): string {
  const dow = getDayOfWeek(dateStr);
  if (dow === 0) return "bg-red-50";
  if (dow === 6) return "bg-blue-50";
  return "";
}

export function AttendanceTable({
  records,
  showEmployee = false,
  editable = false,
}: AttendanceTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="border-b bg-gray-50">
            <th className="px-3 py-2 text-left">日付</th>
            {showEmployee && <th className="px-3 py-2 text-left">社員名</th>}
            <th className="px-3 py-2 text-left">出勤</th>
            <th className="px-3 py-2 text-left">退勤</th>
            <th className="px-3 py-2 text-left">休憩</th>
            <th className="px-3 py-2 text-left">勤務</th>
            <th className="px-3 py-2 text-left">残業</th>
            <th className="px-3 py-2 text-center">状態</th>
          </tr>
        </thead>
        <tbody>
          {records.map((record) => {
            const dow = getDayOfWeek(record.date);
            const dayLabel = `${record.date.substring(5)}(${WEEKDAYS[dow]})`;

            const row = (
              <tr
                key={`${record.date}-${record.employeeId}`}
                className={`border-b hover:bg-gray-100 ${getRowClass(record.date)}`}
              >
                <td className="px-3 py-2">{dayLabel}</td>
                {showEmployee && (
                  <td className="px-3 py-2">{record.employeeName}</td>
                )}
                <td className="px-3 py-2">{formatTime(record.clockIn)}</td>
                <td className="px-3 py-2">{formatTime(record.clockOut)}</td>
                <td className="px-3 py-2">
                  {formatMinutes(record.totalBreakMinutes)}
                </td>
                <td className="px-3 py-2">
                  {formatMinutes(record.totalWorkMinutes)}
                </td>
                <td className="px-3 py-2">
                  {record.overtimeMinutes && record.overtimeMinutes > 0
                    ? formatMinutes(record.overtimeMinutes)
                    : "-"}
                </td>
                <td className="px-3 py-2 text-center">
                  <StatusIcon status={record.status} />
                </td>
              </tr>
            );

            if (editable && record.id) {
              return (
                <Link
                  key={`${record.date}-${record.employeeId}`}
                  href={`/attendance/${record.id}/edit`}
                  className="contents"
                >
                  {row}
                </Link>
              );
            }

            return row;
          })}
        </tbody>
      </table>
    </div>
  );
}
