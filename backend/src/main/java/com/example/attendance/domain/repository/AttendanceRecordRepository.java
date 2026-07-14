package com.example.attendance.domain.repository;

import com.example.attendance.domain.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    @Query("SELECT a FROM AttendanceRecord a LEFT JOIN FETCH a.breakRecords WHERE a.employee.id = :employeeId AND a.date = :date")
    Optional<AttendanceRecord> findByEmployeeIdAndDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Query("SELECT a FROM AttendanceRecord a LEFT JOIN FETCH a.breakRecords LEFT JOIN FETCH a.employee e LEFT JOIN FETCH e.department WHERE e.id = :employeeId AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date")
    List<AttendanceRecord> findByEmployeeIdAndDateBetween(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT a FROM AttendanceRecord a LEFT JOIN FETCH a.breakRecords JOIN FETCH a.employee e LEFT JOIN FETCH e.department " +
            "WHERE a.date BETWEEN :startDate AND :endDate " +
            "AND (:departmentId IS NULL OR e.department.id = :departmentId) " +
            "AND (:employeeName IS NULL OR e.name LIKE CONCAT('%', :employeeName, '%')) " +
            "ORDER BY e.name, a.date")
    List<AttendanceRecord> findAllByFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("departmentId") Long departmentId,
            @Param("employeeName") String employeeName);
}
