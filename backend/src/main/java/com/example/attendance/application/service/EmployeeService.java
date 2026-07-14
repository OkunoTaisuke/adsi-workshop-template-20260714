package com.example.attendance.application.service;

import com.example.attendance.application.dto.EmployeeCreateRequest;
import com.example.attendance.application.dto.EmployeeDetailResponse;
import com.example.attendance.application.dto.EmployeeUpdateRequest;

import java.util.List;

public interface EmployeeService {

    List<EmployeeDetailResponse> findAll();

    EmployeeDetailResponse create(EmployeeCreateRequest request);

    EmployeeDetailResponse update(Long id, EmployeeUpdateRequest request);
}
