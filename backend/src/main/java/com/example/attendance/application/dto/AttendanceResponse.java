package com.example.attendance.application.dto;

import com.example.attendance.domain.model.AttendanceRecord;
import com.example.attendance.domain.model.AttendanceStatus;
import com.example.attendance.domain.model.WorkDuration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AttendanceResponse(
        Long id,
        LocalDate date,
        LocalTime clockIn,
        LocalTime clockOut,
        List<BreakResponse> breaks,
        Integer totalWorkMinutes,
        Integer totalBreakMinutes,
        Integer overtimeMinutes,
        AttendanceStatus status
) {
    public static AttendanceResponse from(AttendanceRecord record) {
        var breaks = record.getBreakRecords() == null
                ? List.<BreakResponse>of()
                : record.getBreakRecords().stream().map(BreakResponse::from).toList();

        var status = WorkDuration.checkStatus(
                record.getClockIn(),
                record.getClockOut(),
                record.getTotalWorkMinutes()
        );

        return new AttendanceResponse(
                record.getId(),
                record.getDate(),
                record.getClockIn(),
                record.getClockOut(),
                breaks,
                record.getTotalWorkMinutes(),
                record.getTotalBreakMinutes(),
                record.getOvertimeMinutes(),
                status
        );
    }

    public static AttendanceResponse empty(LocalDate date) {
        return new AttendanceResponse(null, date, null, null, List.of(), null, null, null, null);
    }
}
