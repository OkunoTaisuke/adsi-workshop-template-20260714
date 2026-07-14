package com.example.attendance.application.dto;

import com.example.attendance.domain.model.AttendanceRecord;
import com.example.attendance.domain.model.AttendanceStatus;
import com.example.attendance.domain.model.WorkDuration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AttendanceDetailResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String departmentName,
        LocalDate date,
        LocalTime clockIn,
        LocalTime clockOut,
        List<BreakResponse> breaks,
        Integer totalWorkMinutes,
        Integer totalBreakMinutes,
        Integer overtimeMinutes,
        AttendanceStatus status
) {
    public static AttendanceDetailResponse from(AttendanceRecord record) {
        var breaks = record.getBreakRecords() == null
                ? List.<BreakResponse>of()
                : record.getBreakRecords().stream().map(BreakResponse::from).toList();

        var status = WorkDuration.checkStatus(
                record.getClockIn(),
                record.getClockOut(),
                record.getTotalWorkMinutes()
        );

        var employee = record.getEmployee();
        var departmentName = employee.getDepartment() != null
                ? employee.getDepartment().getName()
                : null;

        return new AttendanceDetailResponse(
                record.getId(),
                employee.getId(),
                employee.getName(),
                departmentName,
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

    public static AttendanceDetailResponse empty(LocalDate date, Long employeeId, String employeeName, String departmentName) {
        return new AttendanceDetailResponse(null, employeeId, employeeName, departmentName, date, null, null, List.of(), null, null, null, null);
    }
}
