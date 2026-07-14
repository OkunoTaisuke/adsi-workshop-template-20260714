package com.example.attendance.application.dto;

import com.example.attendance.domain.model.BreakRecord;

import java.time.LocalTime;

public record BreakResponse(
        Long id,
        LocalTime breakStart,
        LocalTime breakEnd
) {
    public static BreakResponse from(BreakRecord breakRecord) {
        return new BreakResponse(
                breakRecord.getId(),
                breakRecord.getBreakStart(),
                breakRecord.getBreakEnd()
        );
    }
}
