"use client";

import type { AttendanceStatus } from "@/lib/types";

interface StatusIconProps {
  status: AttendanceStatus | null;
}

export function StatusIcon({ status }: StatusIconProps) {
  if (!status) {
    return <span className="text-gray-300">-</span>;
  }

  if (status === "OK") {
    return <span className="text-green-500 font-bold" title="正常">&#x2714;</span>;
  }

  const labels: Record<string, string> = {
    LATE_START: "コアタイム遅刻",
    EARLY_LEAVE: "コアタイム早退",
    SHORT_HOURS: "勤務時間不足",
  };

  return (
    <span className="text-yellow-500 font-bold" title={labels[status] || ""}>
      &#x26A0;
    </span>
  );
}
