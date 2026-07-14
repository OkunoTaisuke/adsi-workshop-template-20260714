package com.example.attendance.application.service;

import com.example.attendance.application.dto.AttendanceDetailResponse;
import com.example.attendance.application.dto.AttendanceResponse;
import com.example.attendance.application.dto.AttendanceUpdateRequest;
import com.example.attendance.application.dto.BreakEndRequest;
import com.example.attendance.application.dto.BreakStartRequest;
import com.example.attendance.application.dto.ClockInRequest;
import com.example.attendance.application.dto.ClockOutRequest;
import com.example.attendance.domain.model.Employee;

import java.util.List;

public interface AttendanceService {

    AttendanceResponse clockIn(Employee employee, ClockInRequest request);

    AttendanceResponse clockOut(Employee employee, ClockOutRequest request);

    AttendanceResponse breakStart(Employee employee, BreakStartRequest request);

    AttendanceResponse breakEnd(Employee employee, BreakEndRequest request);

    AttendanceResponse getToday(Employee employee);

    List<AttendanceDetailResponse> getMyAttendance(Employee employee, int year, int month);

    List<AttendanceDetailResponse> getAllAttendance(int year, int month, Long departmentId, String employeeName);

    AttendanceResponse updateAttendance(Employee employee, Long id, AttendanceUpdateRequest request);
}
