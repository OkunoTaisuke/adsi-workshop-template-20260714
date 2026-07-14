"use client";

interface MonthPickerProps {
  year: number;
  month: number;
  onChange: (year: number, month: number) => void;
}

export function MonthPicker({ year, month, onChange }: MonthPickerProps) {
  const handlePrev = () => {
    if (month === 1) {
      onChange(year - 1, 12);
    } else {
      onChange(year, month - 1);
    }
  };

  const handleNext = () => {
    if (month === 12) {
      onChange(year + 1, 1);
    } else {
      onChange(year, month + 1);
    }
  };

  return (
    <div className="flex items-center gap-3">
      <button
        onClick={handlePrev}
        className="px-2 py-1 text-gray-600 hover:text-gray-900 border rounded"
      >
        &#x25C0;
      </button>
      <span className="text-lg font-semibold">
        {year}年{month}月
      </span>
      <button
        onClick={handleNext}
        className="px-2 py-1 text-gray-600 hover:text-gray-900 border rounded"
      >
        &#x25B6;
      </button>
    </div>
  );
}
