package com.example.attendance.application.dto;

import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.Role;

public record EmployeeResponse(
        Long id,
        String email,
        String name,
        Role role
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmail(),
                employee.getName(),
                employee.getRole()
        );
    }
}
