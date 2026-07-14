package com.example.attendance.domain.repository;

import com.example.attendance.domain.model.BreakRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BreakRecordRepository extends JpaRepository<BreakRecord, Long> {

    List<BreakRecord> findByAttendanceRecordId(Long attendanceRecordId);

    void deleteByAttendanceRecordId(Long attendanceRecordId);
}
