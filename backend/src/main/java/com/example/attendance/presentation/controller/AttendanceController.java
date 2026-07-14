package com.example.attendance.presentation.controller;

import com.example.attendance.application.dto.AttendanceDetailResponse;
import com.example.attendance.application.dto.AttendanceResponse;
import com.example.attendance.application.dto.AttendanceUpdateRequest;
import com.example.attendance.application.dto.BreakEndRequest;
import com.example.attendance.application.dto.BreakStartRequest;
import com.example.attendance.application.dto.ClockInRequest;
import com.example.attendance.application.dto.ClockOutRequest;
import com.example.attendance.application.service.AttendanceService;
import com.example.attendance.domain.model.Employee;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceResponse> clockIn(
            @AuthenticationPrincipal Employee employee,
            @Valid @RequestBody ClockInRequest request) {
        return ResponseEntity.ok(attendanceService.clockIn(employee, request));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceResponse> clockOut(
            @AuthenticationPrincipal Employee employee,
            @Valid @RequestBody ClockOutRequest request) {
        return ResponseEntity.ok(attendanceService.clockOut(employee, request));
    }

    @PostMapping("/break-start")
    public ResponseEntity<AttendanceResponse> breakStart(
            @AuthenticationPrincipal Employee employee,
            @Valid @RequestBody BreakStartRequest request) {
        return ResponseEntity.ok(attendanceService.breakStart(employee, request));
    }

    @PostMapping("/break-end")
    public ResponseEntity<AttendanceResponse> breakEnd(
            @AuthenticationPrincipal Employee employee,
            @Valid @RequestBody BreakEndRequest request) {
        return ResponseEntity.ok(attendanceService.breakEnd(employee, request));
    }

    @GetMapping("/today")
    public ResponseEntity<AttendanceResponse> getToday(
            @AuthenticationPrincipal Employee employee) {
        return ResponseEntity.ok(attendanceService.getToday(employee));
    }

    @GetMapping("/my")
    public ResponseEntity<List<AttendanceDetailResponse>> getMyAttendance(
            @AuthenticationPrincipal Employee employee,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(attendanceService.getMyAttendance(employee, year, month));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AttendanceDetailResponse>> getAllAttendance(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String employeeName) {
        return ResponseEntity.ok(attendanceService.getAllAttendance(year, month, departmentId, employeeName));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @AuthenticationPrincipal Employee employee,
            @PathVariable Long id,
            @Valid @RequestBody AttendanceUpdateRequest request) {
        return ResponseEntity.ok(attendanceService.updateAttendance(employee, id, request));
    }
}
