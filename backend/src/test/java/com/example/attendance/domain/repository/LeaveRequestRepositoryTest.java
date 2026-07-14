package com.example.attendance.domain.repository;

import com.example.attendance.domain.model.LeavePeriodType;
import com.example.attendance.domain.model.LeaveRequest;
import com.example.attendance.domain.model.LeaveStatus;
import com.example.attendance.domain.model.LeaveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LeaveRequestRepositoryTest {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Test
    @DisplayName("findOverlapping: 重複する PENDING 申請を検出する")
    void findOverlapping_overlappingPending_returnsMatches() {
        // Arrange: employee_id=2 (tanaka) from V2 sample data
        var leave = LeaveRequest.builder()
                .employeeId(2L)
                .leaveType(LeaveType.PAID)
                .leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 3))
                .status(LeaveStatus.PENDING)
                .build();
        leaveRequestRepository.save(leave);

        // Act
        var result = leaveRequestRepository.findOverlapping(
                2L,
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 5));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    @DisplayName("findOverlapping: 重複しない期間では空を返す")
    void findOverlapping_noOverlap_returnsEmpty() {
        // Arrange
        var leave = LeaveRequest.builder()
                .employeeId(2L)
                .leaveType(LeaveType.PAID)
                .leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 3))
                .status(LeaveStatus.PENDING)
                .build();
        leaveRequestRepository.save(leave);

        // Act
        var result = leaveRequestRepository.findOverlapping(
                2L,
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 5));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findOverlapping: CANCELLED は検出対象外")
    void findOverlapping_cancelledStatus_notReturned() {
        // Arrange
        var leave = LeaveRequest.builder()
                .employeeId(2L)
                .leaveType(LeaveType.PAID)
                .leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 3))
                .status(LeaveStatus.CANCELLED)
                .build();
        leaveRequestRepository.save(leave);

        // Act
        var result = leaveRequestRepository.findOverlapping(
                2L,
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("cancelOverlapping: 一括キャンセルが正しく動作する")
    void cancelOverlapping_updatesStatus() {
        // Arrange
        var leave1 = LeaveRequest.builder()
                .employeeId(2L)
                .leaveType(LeaveType.PAID)
                .leavePeriodType(LeavePeriodType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 3))
                .status(LeaveStatus.PENDING)
                .build();
        var leave2 = LeaveRequest.builder()
                .employeeId(2L)
                .leaveType(LeaveType.PAID)
                .leavePeriodType(LeavePeriodType.AM_ONLY)
                .startDate(LocalDate.of(2026, 8, 2))
                .endDate(LocalDate.of(2026, 8, 2))
                .status(LeaveStatus.APPROVED)
                .build();
        leaveRequestRepository.save(leave1);
        leaveRequestRepository.save(leave2);

        // Act
        int count = leaveRequestRepository.cancelOverlapping(
                2L,
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                LeaveStatus.CANCELLED);

        // Assert
        assertThat(count).isEqualTo(2);
    }
}
