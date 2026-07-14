package com.example.attendance.application.service;

import com.example.attendance.application.dto.EmployeeResponse;
import com.example.attendance.application.dto.LoginRequest;
import com.example.attendance.application.dto.LoginResponse;
import com.example.attendance.domain.model.Employee;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    EmployeeResponse getMe(Employee employee);
}
