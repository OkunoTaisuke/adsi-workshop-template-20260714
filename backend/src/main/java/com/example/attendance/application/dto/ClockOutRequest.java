package com.example.attendance.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ClockOutRequest(
        @NotNull LocalTime clockOut
) {
}
