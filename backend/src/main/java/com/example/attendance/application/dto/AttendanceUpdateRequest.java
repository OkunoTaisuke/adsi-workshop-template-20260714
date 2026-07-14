package com.example.attendance.application.dto;

import jakarta.validation.Valid;

import java.time.LocalTime;
import java.util.List;

public record AttendanceUpdateRequest(
        LocalTime clockIn,
        LocalTime clockOut,
        @Valid List<BreakUpdateRequest> breaks
) {
}
