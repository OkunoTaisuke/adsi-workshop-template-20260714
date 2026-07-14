package com.example.attendance.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record BreakStartRequest(
        @NotNull LocalTime breakStart
) {
}
