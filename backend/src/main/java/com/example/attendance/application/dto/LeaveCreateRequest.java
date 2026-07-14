package com.example.attendance.application.dto;

import com.example.attendance.domain.model.LeavePeriodType;
import com.example.attendance.domain.model.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveCreateRequest(
        @NotNull LeaveType leaveType,
        @NotNull LeavePeriodType leavePeriodType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 500) String reason
) {}
