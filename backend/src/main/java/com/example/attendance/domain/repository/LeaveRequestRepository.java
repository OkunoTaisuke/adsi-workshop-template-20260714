package com.example.attendance.domain.repository;

import com.example.attendance.domain.model.LeaveRequest;
import com.example.attendance.domain.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<LeaveRequest> findByEmployeeIdInOrderByCreatedAtDesc(Set<Long> employeeIds);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employeeId = :employeeId " +
           "AND lr.status IN :statuses " +
           "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("statuses") List<LeaveStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("UPDATE LeaveRequest lr SET lr.status = :newStatus, lr.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE lr.employeeId = :employeeId " +
           "AND lr.status IN :statuses " +
           "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    int cancelOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("statuses") List<LeaveStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("newStatus") LeaveStatus newStatus);
}
