"use client";

import { useState } from "react";

interface ClockPopupProps {
  title: string;
  defaultTime: string;
  onConfirm: (time: string) => void;
  onCancel: () => void;
  warning?: string | null;
}

export function ClockPopup({
  title,
  defaultTime,
  onConfirm,
  onCancel,
  warning,
}: ClockPopupProps) {
  const [time, setTime] = useState(defaultTime);

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-80 shadow-xl">
        <h3 className="text-lg font-semibold mb-4">{title}</h3>

        {warning && (
          <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-800">
            {warning}
          </div>
        )}

        <div className="mb-4">
          <label className="block text-sm text-gray-600 mb-1">時刻</label>
          <input
            type="time"
            value={time}
            onChange={(e) => setTime(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2 text-lg"
          />
        </div>

        <div className="flex gap-3 justify-end">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm text-gray-600 border border-gray-300 rounded hover:bg-gray-50"
          >
            キャンセル
          </button>
          <button
            onClick={() => onConfirm(time)}
            className="px-4 py-2 text-sm text-white bg-blue-600 rounded hover:bg-blue-700"
          >
            送信
          </button>
        </div>
      </div>
    </div>
  );
}
