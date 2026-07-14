package com.example.attendance.application.dto;

import com.example.attendance.domain.model.Employee;

import java.util.Set;

public record EmployeeResponse(
        Long id,
        String email,
        String name,
        Set<String> roles,
        Long departmentId
) {
    public static EmployeeResponse from(Employee employee, Set<String> roles) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmail(),
                employee.getName(),
                roles,
                employee.getDepartmentId()
        );
    }
}
