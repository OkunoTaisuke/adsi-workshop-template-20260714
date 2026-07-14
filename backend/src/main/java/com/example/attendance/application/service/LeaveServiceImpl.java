package com.example.attendance.application.service;

import com.example.attendance.application.dto.LeaveCreateRequest;
import com.example.attendance.application.dto.LeaveRejectRequest;
import com.example.attendance.application.dto.LeaveResponse;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.LeaveRequest;
import com.example.attendance.domain.model.LeaveStatus;
import com.example.attendance.domain.model.Role;
import com.example.attendance.domain.repository.EmployeeRepository;
import com.example.attendance.domain.repository.EmployeeRoleRepository;
import com.example.attendance.domain.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;

    public LeaveServiceImpl(LeaveRequestRepository leaveRequestRepository,
                            EmployeeRepository employeeRepository,
                            EmployeeRoleRepository employeeRoleRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.employeeRoleRepository = employeeRoleRepository;
    }

    @Override
    public LeaveResponse create(LeaveCreateRequest request, Employee applicant) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("開始日は終了日以前である必要があります");
        }

        leaveRequestRepository.cancelOverlapping(
                applicant.getId(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                request.startDate(),
                request.endDate(),
                LeaveStatus.CANCELLED);

        boolean isManager = employeeRoleRepository.existsByEmployeeIdAndRole(
                applicant.getId(), Role.MANAGER);

        var leaveRequest = LeaveRequest.builder()
                .employeeId(applicant.getId())
                .leaveType(request.leaveType())
                .leavePeriodType(request.leavePeriodType())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .status(isManager ? LeaveStatus.APPROVED : LeaveStatus.PENDING)
                .approvedBy(isManager ? applicant.getId() : null)
                .approvedAt(isManager ? LocalDateTime.now() : null)
                .build();

        var saved = leaveRequestRepository.save(leaveRequest);
        return LeaveResponse.from(saved, applicant.getName(), isManager ? applicant.getName() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> findMyLeaves(Employee employee) {
        var leaves = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId());
        var approverIds = leaves.stream()
                .map(LeaveRequest::getApprovedBy)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        var approverMap = buildNameMap(approverIds);

        return leaves.stream()
                .map(lr -> LeaveResponse.from(lr, employee.getName(), approverMap.get(lr.getApprovedBy())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> findSubordinateLeaves(Employee manager) {
        var subordinates = employeeRepository.findByDepartmentId(manager.getDepartmentId());
        var subordinateIds = subordinates.stream()
                .map(Employee::getId)
                .filter(id -> !id.equals(manager.getId()))
                .collect(Collectors.toSet());

        if (subordinateIds.isEmpty()) {
            return List.of();
        }

        var leaves = leaveRequestRepository.findByEmployeeIdInOrderByCreatedAtDesc(subordinateIds);
        var employeeMap = subordinates.stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName));
        var approverIds = leaves.stream()
                .map(LeaveRequest::getApprovedBy)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        var approverMap = buildNameMap(approverIds);

        return leaves.stream()
                .map(lr -> LeaveResponse.from(lr, employeeMap.get(lr.getEmployeeId()), approverMap.get(lr.getApprovedBy())))
                .toList();
    }

    @Override
    public LeaveResponse approve(Long leaveId, Employee manager) {
        var leaveRequest = findLeaveOrThrow(leaveId);
        validatePendingStatus(leaveRequest);
        validateManagerAuthority(leaveRequest, manager);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(manager.getId());
        leaveRequest.setApprovedAt(LocalDateTime.now());
        var saved = leaveRequestRepository.save(leaveRequest);

        var applicant = employeeRepository.findById(saved.getEmployeeId()).orElse(null);
        return LeaveResponse.from(saved, applicant != null ? applicant.getName() : null, manager.getName());
    }

    @Override
    public LeaveResponse reject(Long leaveId, LeaveRejectRequest request, Employee manager) {
        var leaveRequest = findLeaveOrThrow(leaveId);
        validatePendingStatus(leaveRequest);
        validateManagerAuthority(leaveRequest, manager);

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(manager.getId());
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setRejectionReason(request.reason());
        var saved = leaveRequestRepository.save(leaveRequest);

        var applicant = employeeRepository.findById(saved.getEmployeeId()).orElse(null);
        return LeaveResponse.from(saved, applicant != null ? applicant.getName() : null, manager.getName());
    }

    @Override
    public LeaveResponse cancel(Long leaveId, Employee employee) {
        var leaveRequest = findLeaveOrThrow(leaveId);
        validatePendingStatus(leaveRequest);

        if (!leaveRequest.getEmployeeId().equals(employee.getId())) {
            throw new ForbiddenException("本人の申請のみキャンセルできます");
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        var saved = leaveRequestRepository.save(leaveRequest);

        var approverName = saved.getApprovedBy() != null
                ? employeeRepository.findById(saved.getApprovedBy()).map(Employee::getName).orElse(null)
                : null;
        return LeaveResponse.from(saved, employee.getName(), approverName);
    }

    private LeaveRequest findLeaveOrThrow(Long leaveId) {
        return leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("休暇申請が見つかりません"));
    }

    private void validatePendingStatus(LeaveRequest leaveRequest) {
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException("この申請は既に処理済みです");
        }
    }

    private void validateManagerAuthority(LeaveRequest leaveRequest, Employee manager) {
        boolean hasManagerRole = employeeRoleRepository.existsByEmployeeIdAndRole(
                manager.getId(), Role.MANAGER);
        if (!hasManagerRole) {
            throw new ForbiddenException("承認権限がありません");
        }

        var applicant = employeeRepository.findById(leaveRequest.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("申請者が見つかりません"));

        if (applicant.getDepartmentId() == null || manager.getDepartmentId() == null) {
            throw new ForbiddenException("部署が未設定のため承認できません");
        }

        if (!applicant.getDepartmentId().equals(manager.getDepartmentId())) {
            throw new ForbiddenException("同一部署の申請のみ承認できます");
        }
    }

    private Map<Long, String> buildNameMap(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName));
    }
}
