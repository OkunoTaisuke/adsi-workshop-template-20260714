package com.example.attendance.domain.repository;

import com.example.attendance.domain.model.EmployeeRole;
import com.example.attendance.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, Long> {

    List<EmployeeRole> findByEmployeeId(Long employeeId);

    List<EmployeeRole> findByEmployeeIdIn(Set<Long> employeeIds);

    @Modifying
    @Query("DELETE FROM EmployeeRole er WHERE er.employeeId = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") Long employeeId);

    boolean existsByEmployeeIdAndRole(Long employeeId, Role role);
}
