package com.example.attendance.application.service;

import com.example.attendance.application.dto.LeaveCreateRequest;
import com.example.attendance.application.dto.LeaveRejectRequest;
import com.example.attendance.application.dto.LeaveResponse;
import com.example.attendance.domain.model.Employee;

import java.util.List;

public interface LeaveService {

    LeaveResponse create(LeaveCreateRequest request, Employee applicant);

    List<LeaveResponse> findMyLeaves(Employee employee);

    List<LeaveResponse> findSubordinateLeaves(Employee manager);

    LeaveResponse approve(Long leaveId, Employee manager);

    LeaveResponse reject(Long leaveId, LeaveRejectRequest request, Employee manager);

    LeaveResponse cancel(Long leaveId, Employee employee);
}
