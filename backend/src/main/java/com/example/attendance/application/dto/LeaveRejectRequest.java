package com.example.attendance.application.dto;

import jakarta.validation.constraints.Size;

public record LeaveRejectRequest(
        @Size(max = 500) String reason
) {}
