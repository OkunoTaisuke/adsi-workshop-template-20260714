"use client";

import { useState } from "react";
import type { AttendanceResponse } from "@/lib/types";
import { ClockPopup } from "./ClockPopup";
import * as api from "@/lib/attendance-api";

interface ClockButtonsProps {
  attendance: AttendanceResponse | null;
  onUpdate: (updated: AttendanceResponse) => void;
}

type PopupType = "clockIn" | "clockOut" | "breakStart" | "breakEnd" | null;

function getCurrentTime(): string {
  const now = new Date();
  return `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
}

export function ClockButtons({ attendance, onUpdate }: ClockButtonsProps) {
  const [popup, setPopup] = useState<PopupType>(null);
  const [warning, setWarning] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const hasActiveBreak = attendance?.breaks.some((b) => b.breakEnd === null) ?? false;
  const isClockedIn = !!attendance?.clockIn;
  const isClockedOut = !!attendance?.clockOut;

  const handleButtonClick = (type: PopupType) => {
    setError(null);
    setWarning(null);

    if (type === "breakStart" && hasActiveBreak) {
      setWarning("未終了の休憩があります。先に休憩終了してください。");
      setPopup(type);
      return;
    }
    if (type === "clockOut" && hasActiveBreak) {
      setWarning("休憩が終了していません。先に休憩終了してください。");
      setPopup(type);
      return;
    }

    setPopup(type);
  };

  const handleConfirm = async (time: string) => {
    setLoading(true);
    setError(null);
    try {
      let result: AttendanceResponse;
      switch (popup) {
        case "clockIn":
          result = await api.clockIn(time);
          break;
        case "clockOut":
          result = await api.clockOut(time);
          break;
        case "breakStart":
          result = await api.breakStart(time);
          break;
        case "breakEnd":
          result = await api.breakEnd(time);
          break;
        default:
          return;
      }
      onUpdate(result);
      setPopup(null);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "エラーが発生しました";
      setError(msg);
      setPopup(null);
    } finally {
      setLoading(false);
    }
  };

  const popupTitles: Record<string, string> = {
    clockIn: "出勤打刻",
    clockOut: "退勤打刻",
    breakStart: "休憩開始",
    breakEnd: "休憩終了",
  };

  return (
    <div>
      <div className="flex gap-3 flex-wrap">
        {!isClockedIn && (
          <button
            onClick={() => handleButtonClick("clockIn")}
            disabled={loading}
            className="px-5 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium"
          >
            出勤
          </button>
        )}
        {isClockedIn && !isClockedOut && (
          <>
            <button
              onClick={() => handleButtonClick("clockOut")}
              disabled={loading}
              className="px-5 py-2.5 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 font-medium"
            >
              退勤
            </button>
            {!hasActiveBreak && (
              <button
                onClick={() => handleButtonClick("breakStart")}
                disabled={loading}
                className="px-5 py-2.5 bg-yellow-500 text-white rounded-lg hover:bg-yellow-600 disabled:opacity-50 font-medium"
              >
                休憩開始
              </button>
            )}
            {hasActiveBreak && (
              <button
                onClick={() => handleButtonClick("breakEnd")}
                disabled={loading}
                className="px-5 py-2.5 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 font-medium"
              >
                休憩終了
              </button>
            )}
          </>
        )}
        {isClockedOut && (
          <div className="text-gray-500 text-sm py-2">
            本日の勤務は終了しました
          </div>
        )}
      </div>

      {error && (
        <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">
          {error}
        </div>
      )}

      {popup && (
        <ClockPopup
          title={popupTitles[popup]}
          defaultTime={getCurrentTime()}
          onConfirm={handleConfirm}
          onCancel={() => setPopup(null)}
          warning={warning}
        />
      )}
    </div>
  );
}
