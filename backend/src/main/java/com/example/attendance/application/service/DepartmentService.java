package com.example.attendance.application.service;

import com.example.attendance.application.dto.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    List<DepartmentResponse> findAll();
}
