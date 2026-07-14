package com.example.attendance.application.service;

import com.example.attendance.application.dto.DepartmentRequest;
import com.example.attendance.application.dto.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    List<DepartmentResponse> findAll();

    DepartmentResponse create(DepartmentRequest request);

    DepartmentResponse update(Long id, DepartmentRequest request);

    void delete(Long id);
}
