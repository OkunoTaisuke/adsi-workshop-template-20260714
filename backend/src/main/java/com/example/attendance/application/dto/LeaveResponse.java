package com.example.attendance.application.dto;

import com.example.attendance.domain.model.LeaveRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String leaveType,
        String leavePeriodType,
        String leavePeriodTypeDisplay,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String status,
        String statusDisplay,
        String approvedByName,
        LocalDateTime approvedAt,
        String rejectionReason,
        LocalDateTime createdAt
) {
    public static LeaveResponse from(LeaveRequest lr, String employeeName, String approvedByName) {
        return new LeaveResponse(
                lr.getId(),
                lr.getEmployeeId(),
                employeeName,
                lr.getLeaveType().name(),
                lr.getLeavePeriodType().name(),
                periodTypeDisplay(lr.getLeavePeriodType().name()),
                lr.getStartDate(),
                lr.getEndDate(),
                lr.getReason(),
                lr.getStatus().name(),
                statusDisplay(lr.getStatus().name()),
                approvedByName,
                lr.getApprovedAt(),
                lr.getRejectionReason(),
                lr.getCreatedAt()
        );
    }

    private static String periodTypeDisplay(String type) {
        return switch (type) {
            case "FULL_DAY" -> "全日";
            case "AM_ONLY" -> "午前休";
            case "PM_ONLY" -> "午後休";
            default -> type;
        };
    }

    private static String statusDisplay(String status) {
        return switch (status) {
            case "PENDING" -> "申請中";
            case "APPROVED" -> "承認";
            case "REJECTED" -> "却下";
            case "CANCELLED" -> "キャンセル";
            default -> status;
        };
    }
}
