package com.example.attendance.domain.model;

import java.time.LocalTime;
import java.util.List;

public record WorkDuration(
        Integer totalWorkMinutes,
        Integer totalBreakMinutes,
        Integer overtimeMinutes
) {

    private static final int STANDARD_WORK_MINUTES = 480;

    public static WorkDuration calculate(LocalTime clockIn, LocalTime clockOut, List<BreakRecord> breaks) {
        if (clockIn == null || clockOut == null) {
            return new WorkDuration(null, null, null);
        }

        int grossMinutes = calculateMinutesBetween(clockIn, clockOut);
        int breakMinutes = calculateBreakMinutes(breaks);
        int workMinutes = Math.max(0, grossMinutes - breakMinutes);
        int overtime = Math.max(0, workMinutes - STANDARD_WORK_MINUTES);

        return new WorkDuration(workMinutes, breakMinutes, overtime);
    }

    private static int calculateMinutesBetween(LocalTime start, LocalTime end) {
        int startMinutes = start.getHour() * 60 + start.getMinute();
        int endMinutes = end.getHour() * 60 + end.getMinute();

        if (endMinutes <= startMinutes) {
            endMinutes += 24 * 60;
        }

        return endMinutes - startMinutes;
    }

    private static int calculateBreakMinutes(List<BreakRecord> breaks) {
        if (breaks == null) {
            return 0;
        }
        return breaks.stream()
                .filter(b -> b.getBreakStart() != null && b.getBreakEnd() != null)
                .mapToInt(b -> calculateMinutesBetween(b.getBreakStart(), b.getBreakEnd()))
                .sum();
    }

    public static AttendanceStatus checkStatus(LocalTime clockIn, LocalTime clockOut, Integer totalWorkMinutes) {
        if (clockIn == null || clockOut == null || totalWorkMinutes == null) {
            return null;
        }

        LocalTime coreStart = LocalTime.of(10, 0);
        LocalTime coreEnd = LocalTime.of(15, 0);

        if (totalWorkMinutes < STANDARD_WORK_MINUTES) {
            return AttendanceStatus.SHORT_HOURS;
        }
        if (clockIn.isAfter(coreStart)) {
            return AttendanceStatus.LATE_START;
        }
        if (clockOut.isBefore(coreEnd)) {
            return AttendanceStatus.EARLY_LEAVE;
        }

        return AttendanceStatus.OK;
    }
}
