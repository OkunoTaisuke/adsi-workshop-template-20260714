package com.example.attendance.application.dto;

import java.util.Set;

public record EmployeeDetailResponse(
        Long id,
        String name,
        String email,
        String departmentName,
        Long departmentId,
        Set<String> roles
) {}
