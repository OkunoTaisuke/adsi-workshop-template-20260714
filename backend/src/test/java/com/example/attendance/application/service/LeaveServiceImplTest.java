package com.example.attendance.application.service;

import com.example.attendance.application.dto.LeaveCreateRequest;
import com.example.attendance.application.dto.LeaveRejectRequest;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.LeavePeriodType;
import com.example.attendance.domain.model.LeaveRequest;
import com.example.attendance.domain.model.LeaveStatus;
import com.example.attendance.domain.model.LeaveType;
import com.example.attendance.domain.model.Role;
import com.example.attendance.domain.repository.EmployeeRepository;
import com.example.attendance.domain.repository.EmployeeRoleRepository;
import com.example.attendance.domain.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaveServiceImplTest {

    private LeaveRequestRepository leaveRequestRepository;
    private EmployeeRepository employeeRepository;
    private EmployeeRoleRepository employeeRoleRepository;
    private LeaveServiceImpl service;

    private Employee user;
    private Employee manager;

    @BeforeEach
    void setUp() {
        leaveRequestRepository = mock(LeaveRequestRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        employeeRoleRepository = mock(EmployeeRoleRepository.class);
        service = new LeaveServiceImpl(leaveRequestRepository, employeeRepository, employeeRoleRepository);

        user = Employee.builder().id(1L).name("田中太郎").email("tanaka@example.com")
                .departmentId(10L).role(Role.EMPLOYEE).build();
        manager = Employee.builder().id(2L).name("佐藤花子").email("sato@example.com")
                .departmentId(10L).role(Role.EMPLOYEE).build();
    }

    @Test
    @DisplayName("申請作成: 一般ユーザーの申請は PENDING になる")
    void create_normalUser_statusPending() {
        var request = new LeaveCreateRequest(LeaveType.PAID, LeavePeriodType.FULL_DAY,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), "私用");
        when(leaveRequestRepository.cancelOverlapping(any(), any(), any(), any(), any())).thenReturn(0);
        when(employeeRoleRepository.existsByEmployeeIdAndRole(1L, Role.MANAGER)).thenReturn(false);
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> {
            LeaveRequest lr = inv.getArgument(0);
            lr.setId(100L);
            return lr;
        });

        var result = service.create(request, user);

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.employeeName()).isEqualTo("田中太郎");
    }

    @Test
    @DisplayName("申請作成: MANAGER の申請は自動承認される")
    void create_manager_autoApproved() {
        var request = new LeaveCreateRequest(LeaveType.PAID, LeavePeriodType.AM_ONLY,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), null);
        when(leaveRequestRepository.cancelOverlapping(any(), any(), any(), any(), any())).thenReturn(0);
        when(employeeRoleRepository.existsByEmployeeIdAndRole(2L, Role.MANAGER)).thenReturn(true);
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> {
            LeaveRequest lr = inv.getArgument(0);
            lr.setId(101L);
            return lr;
        });

        var result = service.create(request, manager);

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.approvedByName()).isEqualTo("佐藤花子");
    }

    @Test
    @DisplayName("申請作成: 開始日 > 終了日で IllegalArgumentException")
    void create_invalidDates_throwsException() {
        var request = new LeaveCreateRequest(LeaveType.PAID, LeavePeriodType.FULL_DAY,
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 1), null);

        assertThatThrownBy(() -> service.create(request, user))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("申請作成: 重複がある場合は cancelOverlapping が呼ばれる")
    void create_overlapping_callsCancelOverlapping() {
        var request = new LeaveCreateRequest(LeaveType.PAID, LeavePeriodType.FULL_DAY,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), null);
        when(leaveRequestRepository.cancelOverlapping(any(), any(), any(), any(), any())).thenReturn(2);
        when(employeeRoleRepository.existsByEmployeeIdAndRole(1L, Role.MANAGER)).thenReturn(false);
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> {
            LeaveRequest lr = inv.getArgument(0);
            if (lr.getId() == null) lr.setId(101L);
            return lr;
        });

        service.create(request, user);

        verify(leaveRequestRepository).cancelOverlapping(
                eq(1L),
                eq(List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED)),
                eq(LocalDate.of(2026, 8, 1)),
                eq(LocalDate.of(2026, 8, 1)),
                eq(LeaveStatus.CANCELLED));
    }

    @Test
    @DisplayName("承認: 正常に APPROVED になる")
    void approve_valid_statusApproved() {
        var leaveRequest = LeaveRequest.builder().id(1L).employeeId(1L)
                .leaveType(LeaveType.PAID).leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 1))
                .status(LeaveStatus.PENDING).build();
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(employeeRoleRepository.existsByEmployeeIdAndRole(2L, Role.MANAGER)).thenReturn(true);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(user));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.approve(1L, manager);

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.approvedByName()).isEqualTo("佐藤花子");
    }

    @Test
    @DisplayName("承認: 別部署の MANAGER は 403")
    void approve_differentDepartment_throwsForbidden() {
        var otherManager = Employee.builder().id(3L).name("他部署").departmentId(99L).build();
        var leaveRequest = LeaveRequest.builder().id(1L).employeeId(1L)
                .leaveType(LeaveType.PAID).leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 1))
                .status(LeaveStatus.PENDING).build();
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(employeeRoleRepository.existsByEmployeeIdAndRole(3L, Role.MANAGER)).thenReturn(true);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.approve(1L, otherManager))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("承認: PENDING でない申請は 409")
    void approve_notPending_throwsConflict() {
        var leaveRequest = LeaveRequest.builder().id(1L).employeeId(1L)
                .leaveType(LeaveType.PAID).leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 1))
                .status(LeaveStatus.APPROVED).build();
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> service.approve(1L, manager))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("却下: 正常に REJECTED になる")
    void reject_valid_statusRejected() {
        var leaveRequest = LeaveRequest.builder().id(1L).employeeId(1L)
                .leaveType(LeaveType.PAID).leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 1))
                .status(LeaveStatus.PENDING).build();
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(employeeRoleRepository.existsByEmployeeIdAndRole(2L, Role.MANAGER)).thenReturn(true);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(user));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.reject(1L, new LeaveRejectRequest("理由あり"), manager);

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.rejectionReason()).isEqualTo("理由あり");
    }

    @Test
    @DisplayName("キャンセル: 本人なら CANCELLED になる")
    void cancel_ownRequest_statusCancelled() {
        var leaveRequest = LeaveRequest.builder().id(1L).employeeId(1L)
                .leaveType(LeaveType.PAID).leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 1))
                .status(LeaveStatus.PENDING).build();
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.cancel(1L, user);

        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("キャンセル: 他人の申請は 403")
    void cancel_otherPerson_throwsForbidden() {
        var leaveRequest = LeaveRequest.builder().id(1L).employeeId(99L)
                .leaveType(LeaveType.PAID).leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 1))
                .status(LeaveStatus.PENDING).build();
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> service.cancel(1L, user))
                .isInstanceOf(ForbiddenException.class);
    }
}
