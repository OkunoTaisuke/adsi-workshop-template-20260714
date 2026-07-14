package com.example.attendance.domain.repository;

import com.example.attendance.domain.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.department WHERE e.email = :email")
    Optional<Employee> findByEmail(@Param("email") String email);

    boolean existsByEmail(String email);

    boolean existsByDepartmentId(Long departmentId);

    List<Employee> findByDepartmentId(Long departmentId);
}
