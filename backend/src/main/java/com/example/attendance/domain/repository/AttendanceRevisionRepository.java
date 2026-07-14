package com.example.attendance.domain.repository;

import com.example.attendance.domain.model.AttendanceRevision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRevisionRepository extends JpaRepository<AttendanceRevision, Long> {
}
