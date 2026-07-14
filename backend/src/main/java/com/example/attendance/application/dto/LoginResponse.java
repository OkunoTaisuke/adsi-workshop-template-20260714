package com.example.attendance.application.dto;

public record LoginResponse(
        String token,
        EmployeeResponse employee
) {}
